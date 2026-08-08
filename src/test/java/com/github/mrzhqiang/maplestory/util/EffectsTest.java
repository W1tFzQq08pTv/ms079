package com.github.mrzhqiang.maplestory.util;

import com.github.mrzhqiang.maplestory.wz.element.ImgdirElement;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    private static ImgdirElement levelElement() {
        return ImgdirElement.of(levelSource());
    }

    private static Element levelSource() {
        return new Element(Tag.valueOf("imgdir"), "").attr("name", "1");
    }
}
