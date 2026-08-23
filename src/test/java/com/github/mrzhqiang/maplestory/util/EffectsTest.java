package com.github.mrzhqiang.maplestory.util;

import com.github.mrzhqiang.maplestory.wz.element.ImgdirElement;
import client.MapleBuffStat;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;
import server.MapleStatEffect;
import tools.Pair;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EffectsTest {

    @Test
    public void defaultsAttackSkillsToOneTarget() {
        ImgdirElement level = levelElement();

        assertEquals(1, Effects.ofSkill(1001004, false, level).getMobCount());
        assertEquals(0, Effects.ofItem(2000000, level).getMobCount());
    }

    @Test
    public void preservesExplicitSkillTargetCount() {
        Element source = levelSource();
        source.appendElement("int")
                .attr("name", "mobCount")
                .attr("value", "3");

        assertEquals(3, Effects.ofSkill(1001004, false, ImgdirElement.of(source)).getMobCount());
    }

    @Test
    public void ironBodyOnlyConflictsWithHeroBuffsThatAlsoModifyPhysicalDefense() {
        Set<MapleBuffStat> ironBody = buffStats(1001003, statLevel("time", 300, "pdd", 40));
        Set<MapleBuffStat> rage = buffStats(1101006,
                statLevel("time", 160, "pad", 12, "pdd", -12));
        Set<MapleBuffStat> combo = buffStats(1111002, statLevel("time", 200, "x", 5));
        Set<MapleBuffStat> mapleWarrior = buffStats(1121000, statLevel("time", 900, "x", 15));
        Set<MapleBuffStat> stance = buffStats(1121002, statLevel("time", 300, "prop", 90));
        Set<MapleBuffStat> enrage = buffStats(1121010, statLevel("time", 240, "pad", 26));

        assertEquals(EnumSet.of(MapleBuffStat.WDEF), ironBody);
        assertTrue(rage.contains(MapleBuffStat.WDEF));
        assertFalse(disjoint(ironBody, rage));
        assertTrue(disjoint(ironBody, combo));
        assertTrue(disjoint(ironBody, mapleWarrior));
        assertTrue(disjoint(ironBody, stance));
        assertTrue(disjoint(ironBody, enrage));
    }

    private static ImgdirElement levelElement() {
        return ImgdirElement.of(levelSource());
    }

    private static Element levelSource() {
        return new Element(Tag.valueOf("imgdir"), "").attr("name", "1");
    }

    private static ImgdirElement statLevel(Object... stats) {
        Element source = levelSource();
        for (int i = 0; i < stats.length; i += 2) {
            source.appendElement("int")
                    .attr("name", stats[i].toString())
                    .attr("value", stats[i + 1].toString());
        }
        return ImgdirElement.of(source);
    }

    private static Set<MapleBuffStat> buffStats(int skillId, ImgdirElement level) {
        MapleStatEffect effect = Effects.ofSkill(skillId, true, level);
        Set<MapleBuffStat> result = EnumSet.noneOf(MapleBuffStat.class);
        for (Pair<MapleBuffStat, Integer> statup : effect.getStatups()) {
            result.add(statup.getLeft());
        }
        return result;
    }

    private static boolean disjoint(Set<MapleBuffStat> left, Set<MapleBuffStat> right) {
        return java.util.Collections.disjoint(left, right);
    }
}
