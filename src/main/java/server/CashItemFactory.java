package server;

import com.github.mrzhqiang.maplestory.wz.WzData;
import com.github.mrzhqiang.maplestory.wz.WzElement;
import com.github.mrzhqiang.maplestory.wz.WzFile;
import com.github.mrzhqiang.maplestory.wz.element.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.CashItemInfo.CashModInfo;

import java.util.*;
import java.util.stream.Collectors;

public class CashItemFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CashItemFactory.class);

    private final static CashItemFactory instance = new CashItemFactory();
    private final static int[] BEST_ITEMS = new int[]{50400041, 50400016, 50400135, 50400061, 20800204};
    private static final int NO_OVERRIDE = -1;
    private static final int NO_MARK = -2;

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
        // This private server intentionally exposes the complete v079 commodity catalog.
        // A show-up-only override keeps the CS_OPEN packet below the legacy packet limit;
        // all other fields continue to come from Commodity.img.
        for (int sn : itemStats.keySet()) {
            itemMods.put(sn, new CashModInfo(sn, 0, NO_MARK, true, 0, NO_OVERRIDE,
                    false, 0, NO_OVERRIDE, 0, 0, 0, 0, 0, 0));
        }
        initialized = true;
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
