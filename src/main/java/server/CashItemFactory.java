package server;

import client.inventory.IItem;
import com.github.mrzhqiang.maplestory.domain.DCashShopModifiedItem;
import com.github.mrzhqiang.maplestory.domain.query.QDCashShopModifiedItem;
import com.github.mrzhqiang.maplestory.wz.WzData;
import com.github.mrzhqiang.maplestory.wz.WzElement;
import com.github.mrzhqiang.maplestory.wz.WzFile;
import com.github.mrzhqiang.maplestory.wz.element.Elements;
import database.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.CashItemInfo.CashModInfo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.stream.Collectors;

public class CashItemFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CashItemFactory.class);

    private final static CashItemFactory instance = new CashItemFactory();
    private final static int[] BEST_ITEMS = new int[]{50100010, 50100010, 50100010, 50100010, 50100010};
    // Package SNs explicitly enabled by the historical v079 CS_OPEN catalog.
    private static final Set<Integer> HISTORICAL_CATALOG_PACKAGES = historicalCatalogPackages();

    private static final Map<Integer, List<CashItemInfo>> CASH_PACKAGES = new HashMap<>();

    private boolean initialized = false;

    private final Map<Integer, CashItemInfo> itemStats = new HashMap<>();
    private final Map<Integer, List<CashItemInfo>> itemPackage = new HashMap<>();
    private final Map<Integer, CashModInfo> itemMods = new HashMap<>();
    private final Map<Integer, Integer> idLookup = new HashMap<>();
    private final Map<Integer, List<CashItemInfo>> itemsById = new HashMap<>();

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
        for (int i : itemStats.keySet()) {
            getModInfo(i);
            getItem(i); //init the modinfo's citem
        }
        initialized = true;
    }

    private void handleCashItemInfo(CashItemInfo info) {
        int sn = info.getSN();
        int id = info.getId();
        if (sn > 0) {
            itemStats.put(sn, info);
            idLookup.put(id, sn);
            itemsById.computeIfAbsent(id, key -> new ArrayList<CashItemInfo>()).add(info);
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

    public CashItemInfo getHistoricalCatalogPackage(int sn) {
        if (getModInfo(sn) != null) {
            return null;
        }
        return resolveHistoricalCatalogPackage(sn, itemStats.get(sn));
    }

    public static boolean isHistoricalCatalogPackageSerial(int sn) {
        return HISTORICAL_CATALOG_PACKAGES.contains(sn);
    }

    static CashItemInfo resolveHistoricalCatalogPackage(int sn, CashItemInfo item) {
        if (item == null || item.getSN() != sn || !HISTORICAL_CATALOG_PACKAGES.contains(sn)
                || getPackageSerials(item.getId()).isEmpty()) {
            return null;
        }
        return item;
    }

    private static Set<Integer> historicalCatalogPackages() {
        Set<Integer> serials = new HashSet<Integer>(Arrays.asList(
                10001747, 10001806, 10001815, 10001818,
                21200000, 21200001, 21200006, 21200012, 21200015,
                70000104, 70000123, 70000125, 70000137, 70000138));
        addRange(serials, 70000002, 70000006);
        addRange(serials, 70000009, 70000014);
        addRange(serials, 70000017, 70000019);
        addRange(serials, 70000044, 70000058);
        addRange(serials, 70000065, 70000072);
        addRange(serials, 70000078, 70000102);
        addRange(serials, 70000110, 70000121);
        addRange(serials, 70000141, 70000148);
        addRange(serials, 70000153, 70000159);
        return Collections.unmodifiableSet(serials);
    }

    private static void addRange(Set<Integer> serials, int first, int last) {
        for (int serial = first; serial <= last; serial++) {
            serials.add(serial);
        }
    }

    public List<CashItemInfo> getPackageItems(int itemId) {
        List<CashItemInfo> list = CASH_PACKAGES.get(itemId);
        if (list != null) {
            return list;
        }

        List<Integer> serials = getPackageSerials(itemId);
        if (serials.isEmpty()) {
            CASH_PACKAGES.put(itemId, Collections.emptyList());
            return Collections.emptyList();
        }

        List<CashItemInfo> packageItems = new ArrayList<>(serials.size());
        for (Integer serial : serials) {
            CashItemInfo packageItem = itemStats.get(serial);
            if (packageItem == null) {
                LOGGER.warn("Cash-shop package item is missing: packageItemId={}, memberSn={}", itemId, serial);
                CASH_PACKAGES.put(itemId, Collections.emptyList());
                return Collections.emptyList();
            }
            packageItems.add(packageItem);
        }
        List<CashItemInfo> result = Collections.unmodifiableList(packageItems);
        CASH_PACKAGES.put(itemId, result);
        return result;
    }

    static List<Integer> getPackageSerials(int itemId) {
        return WzData.ETC.directory()
                .findFile("CashPackage.img")
                .map(WzFile::content)
                .map(it -> it.find(String.valueOf(itemId)))
                .map(it -> it.find("SN"))
                .map(WzElement::childrenStream)
                .map(stream -> stream.map(Elements::ofInt).collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public final CashModInfo getModInfo(int sn) {
        CashModInfo ret = itemMods.get(sn);
        //  LOGGER.debug(itemMods.toString());
        if (ret == null) {
            if (initialized) {
                return null;
            }
            ret = new QDCashShopModifiedItem()
                    .serial.eq(sn)
                    .findOneOrEmpty()
                    .map(item -> new CashModInfo(sn, item.discountPrice, item.mark, item.showup,
                            item.itemid, item.priority, item.packageField, item.period, item.gender, item.count,
                            item.meso, item.unk1, item.unk2, item.unk3, item.extraFlags))
                    .orElse(null);
            itemMods.put(sn, ret);
        }
        return ret;
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
        Integer sn = idLookup.get(itemId);
        return sn == null ? 0 : sn;
    }

    public int getSnFromItem(IItem item) {
        List<CashItemInfo> candidates = itemsById.get(item.getItemId());
        if (candidates == null) {
            return 0;
        }

        List<CashItemInfo> effectiveCandidates = new ArrayList<CashItemInfo>(candidates.size());
        for (CashItemInfo candidate : candidates) {
            CashItemInfo effective = getItem(candidate.getSN());
            effectiveCandidates.add(effective == null ? candidate : effective);
        }
        CashItemInfo match = findMatchingCashItem(item, effectiveCandidates);
        return match == null ? 0 : match.getSN();
    }

    static CashItemInfo findMatchingCashItem(IItem item, List<CashItemInfo> candidates) {
        boolean itemExpires = item.getExpiration() > 0;
        for (CashItemInfo candidate : candidates) {
            if (candidate.getId() == item.getItemId()
                    && candidate.getCount() == item.getQuantity()
                    && (CashShop.cashItemDurationMillis(candidate) > 0) == itemExpires) {
                return candidate;
            }
        }
        return null;
    }

    public final void clearCashShop() {
        itemStats.clear();
        itemPackage.clear();
        itemMods.clear();
        idLookup.clear();
        itemsById.clear();
        CASH_PACKAGES.clear();
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
