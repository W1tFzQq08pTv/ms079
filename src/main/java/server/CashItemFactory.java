package server;

import com.github.mrzhqiang.maplestory.domain.DCashShopModifiedItem;
import com.github.mrzhqiang.maplestory.wz.WzData;
import com.github.mrzhqiang.maplestory.wz.WzElement;
import com.github.mrzhqiang.maplestory.wz.WzFile;
import com.github.mrzhqiang.maplestory.wz.element.Elements;
import io.ebean.DB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.CashItemInfo.CashModInfo;

import java.util.*;
import java.util.stream.Collectors;

public class CashItemFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CashItemFactory.class);

    private final static CashItemFactory instance = new CashItemFactory();
    private final static int[] BEST_ITEMS = new int[]{50400041, 50400016, 50400135, 50400061, 20800204};
    private final Map<Integer, List<CashItemInfo>> cashPackages = new HashMap<>();

    private boolean initialized = false;

    private final Map<Integer, CashItemInfo> itemStats = new HashMap<>();
    private final Map<Integer, List<CashItemInfo>> itemPackage = new HashMap<>();
    private final Map<Integer, CashModInfo> itemMods = new HashMap<>();
    private final Map<Integer, Integer> idLookup = new HashMap<>();

    public static CashItemFactory getInstance() {
        return instance;
    }

    protected CashItemFactory() {
    }

    public void initialize() {
        List<Integer> itemids = WzData.ETC.directory()
                .findFile("Commodity.img")
                .map(WzFile::content)
                .map(WzElement::childrenStream)
                .map(stream -> stream.map(this::cashItemInfoOf)
                        .peek(this::handleCashItemInfo)
                        .map(CashItemInfo::getId)
                        .filter(integer -> integer > 0)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());

        for (int i : itemids) {
            getPackageItems(i);
        }
        loadModifications(DB.find(DCashShopModifiedItem.class).findList(), itemStats.values());
        initialized = true;
    }

    void loadModifications(Collection<DCashShopModifiedItem> modifications) {
        loadModifications(modifications, Collections.<CashItemInfo>emptyList());
    }

    void loadModifications(Collection<DCashShopModifiedItem> modifications,
                           Collection<CashItemInfo> commodities) {
        itemMods.clear();
        Map<Integer, CashItemInfo> commoditiesBySerial = commodities.stream()
                .collect(Collectors.toMap(CashItemInfo::getSN, item -> item));
        int hiddenCount = 0;
        int nonCashItemCount = 0;
        int nonPurchasableCount = 0;
        int mesoPricedCount = 0;
        for (DCashShopModifiedItem item : modifications) {
            if (!Boolean.TRUE.equals(item.showup)) {
                hiddenCount++;
                continue;
            }
            CashItemInfo commodity = commoditiesBySerial.get(item.serial);
            if (commodity != null && !isCashShopItem(commodity)) {
                nonCashItemCount++;
                continue;
            }
            if (item.meso > 0) {
                mesoPricedCount++;
                continue;
            }
            CashModInfo modification = modificationOf(item, commodity);
            CashItemInfo effectiveItem = modification.toCItem(commodity);
            if (commodity != null && (effectiveItem.getPrice() <= 0 || effectiveItem.getCount() <= 0)) {
                nonPurchasableCount++;
                continue;
            }
            itemMods.put(item.serial, modification);
        }
        LOGGER.info("Loaded {} cash-shop modifications; ignored {} hidden rows, {} non-cash rows, {} "
                        + "non-purchasable rows, and {} meso-priced rows",
                itemMods.size(), hiddenCount, nonCashItemCount, nonPurchasableCount, mesoPricedCount);
    }

    private CashModInfo modificationOf(DCashShopModifiedItem item, CashItemInfo commodity) {
        int itemId = commodity == null ? item.itemid : 0;
        int discountPrice = unchanged(item.discountPrice, commodity == null ? 0 : commodity.getPrice())
                ? 0 : item.discountPrice;
        int period = unchanged(item.period, commodity == null ? 0 : commodity.getPeriod())
                ? 0 : item.period;
        int gender = commodity != null && item.gender == commodity.getGender() ? -1 : item.gender;
        int count = unchanged(item.count, commodity == null ? 0 : commodity.getCount())
                ? 0 : item.count;
        int priority = item.priority > 0 ? item.priority : -1;
        CashModInfo modification = new CashModInfo(item.serial, discountPrice, catalogMark(item.mark), item.showup,
                itemId, priority, item.packageField, period, gender, count,
                item.meso, item.unk1, item.unk2, item.unk3, item.extraFlags);
        modification.catalogBucket = catalogBucket(item.serial, commodity);
        return modification;
    }

    private boolean unchanged(int overrideValue, int commodityValue) {
        return overrideValue <= 0 || overrideValue == commodityValue;
    }

    private int catalogMark(int mark) {
        // The bundled catalog uses 0 as its default, not as an explicit NEW badge.
        // Only serialize the client-defined promotional marks (sale, hot, event).
        return mark >= 1 && mark <= 3 ? mark : -2;
    }

    private int catalogBucket(int serial, CashItemInfo commodity) {
        int category = serial / 10000000;
        if (category == 1 && commodity != null) {
            return (category * 1000) + (commodity.getId() / 10000);
        }
        return category * 1000;
    }

    boolean isCashShopItem(CashItemInfo item) {
        return MapleItemInformationProvider.getInstance().isCash(item.getId());
    }

    private void handleCashItemInfo(CashItemInfo info) {
        int sn = info.getSN();
        int id = info.getId();
        if (sn > 0) {
            itemStats.put(sn, info);
            idLookup.put(id, sn);
        }
    }

    private CashItemInfo cashItemInfoOf(WzElement<?> element) {
        int snValue = Elements.findInt(element, "SN");
        int itemIdValue = Elements.findInt(element, "ItemId", 1);
        int countValue = Elements.findInt(element, "Count");
        int priceValue = Elements.findInt(element, "Price");
        int periodValue = Elements.findInt(element, "Period");
        int genderValue = Elements.findInt(element, "Gender", 2);
        int onsaleValue = Elements.findInt(element, "OnSale");
        return new CashItemInfo(itemIdValue, countValue, priceValue, snValue, periodValue, genderValue, onsaleValue > 0);
    }

    public final CashItemInfo getItem(int sn) {
        final CashItemInfo stats = itemStats.get(sn);
        // final CashItemInfo stats = itemStats.get(Integer.valueOf(sn));
        final CashModInfo z = getModInfo(sn);
        if (z != null && z.showUp) {
            return z.toCItem(stats); //null doesnt matter
        }
        if (stats == null || !stats.onSale()) {
            return null;
        }
        //hmm
        return stats;
    }

    /* public final List<CashItemInfo> getPackageItems(int itemId) {
         return itemPackage.get(itemId);
     }*/
    public final List<CashItemInfo> getPackageItems(int itemId) {
        List<CashItemInfo> list = cashPackages.get(itemId);
        if (list != null) {
            return list;
        }

        List<Integer> packageSerialNumbers = getPackageSerialNumbers(itemId);
        List<CashItemInfo> packageItems = packageSerialNumbers.stream()
                // Package components are commonly not sold separately, so use raw commodity data.
                .map(itemStats::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (packageItems.size() != packageSerialNumbers.size()) {
            LOGGER.warn("Cash package {} has unresolved commodity serial numbers; purchase disabled", itemId);
            packageItems = Collections.emptyList();
        }
        packageItems = Collections.unmodifiableList(packageItems);
        cashPackages.put(itemId, packageItems);
        return packageItems;
    }

    final List<Integer> getPackageSerialNumbers(int itemId) {
        return WzData.ETC.directory()
                .findFile("CashPackage.img")
                .map(WzFile::content)
                .map(it -> it.find(String.valueOf(itemId)))
                .map(WzElement::childrenStream)
                .map(stream -> stream.flatMap(WzElement::childrenStream))
                .map(stream -> stream.map(Elements::ofInt).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public final CashModInfo getModInfo(int sn) {
        return itemMods.get(sn);
    }

    public final Collection<CashModInfo> getAllModInfo() {
        if (!initialized) {
            initialize();
        }
        return itemMods.values();
    }

    public final int[] getBestItems() {
        return BEST_ITEMS;
    }

    public int getSnFromId(int itemId) {
        return idLookup.get(itemId);
    }

    public final void clearCashShop() {
        itemStats.clear();
        itemPackage.clear();
        itemMods.clear();
        idLookup.clear();
        cashPackages.clear();
        initialized = false;
        initialize();
    }

    public final int getItemSN(int itemid) {
        for (Map.Entry<Integer, CashItemInfo> ci : itemStats.entrySet()) {
            if (ci.getValue().getId() == itemid) {
                return ci.getValue().getSN();
            }
        }
        return 0;
    }
}
