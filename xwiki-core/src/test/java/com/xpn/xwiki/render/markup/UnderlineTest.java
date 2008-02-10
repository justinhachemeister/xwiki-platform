package com.xpn.xwiki.render.markup;

import java.util.ArrayList;

public class UnderlineTest extends SyntaxTestsParent
{
    protected void setUp()
    {
        super.setUp();
    }

    public void testNotTriggeredWithWhitespace()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("This is not __ underlined __");
        expects.add("This is not __ underlined __");
        tests.add("This is not __underlined __");
        expects.add("This is not __underlined __");
        tests.add("This is not __ underlined__");
        expects.add("This is not __ underlined__");
        test(tests, expects);
    }

    public void testSimple()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("This is __underlined__");
        expects.add("This is <em class=\"underline\">underlined</em>");
        tests.add("This is __a__ letter underlined");
        expects.add("This is <em class=\"underline\">a</em> letter underlined");
        tests.add("__a__");
        expects.add("<em class=\"underline\">a</em>");
        test(tests, expects);
    }

    public void testThree()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("This is __a__ short underline__");
        expects.add("This is <em class=\"underline\">a</em> short underline__");
        tests.add("This is __all __ underlined__");
        expects.add("This is <em class=\"underline\">all __ underlined</em>");
        tests.add("This is __all __underlined__");
        expects.add("This is <em class=\"underline\">all __underlined</em>");
        tests.add("This is __one__underlined__");
        expects.add("This is <em class=\"underline\">one</em>underlined__");
        test(tests, expects);
    }

    public void testMultiple()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("More __underlines__ on a __line__");
        expects.add("More <em class=\"underline\">underlines</em> on a <em class=\"underline\">line</em>");
        test(tests, expects);
    }

    public void testExtraUnderscoresAreInside()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("The extra underscores are ___inside___");
        expects.add("The extra underscores are <em class=\"underline\">_inside_</em>");
        tests.add("The extra underscores are ___ inside ___");
        expects.add("The extra underscores are <em class=\"underline\">_ inside _</em>");
        test(tests, expects);
    }

    public void testWithoutWhitespace()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("This__is__underlined");
        expects.add("This<em class=\"underline\">is</em>underlined");
        test(tests, expects);
    }

    public void testSequence()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("__Eeny__meeny__miny__moe__");
        expects.add("<em class=\"underline\">Eeny</em>meeny<em class=\"underline\">miny</em>moe__");
        tests.add("__ Eeny__meeny__miny__moe__");
        expects.add("__ Eeny<em class=\"underline\">meeny</em>miny<em class=\"underline\">moe</em>");
        test(tests, expects);
    }

    public void testSeveralInARow()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("this is not an underline: ____ ");
        expects.add("this is not an underline: ____ ");
        tests.add("this is an underscore underlined: _____ ");
        expects.add("this is an underscore underlined: <em class=\"underline\">_</em> ");
        tests.add("this is two underscores underlined: ______ ");
        expects.add("this is two underscores underlined: <em class=\"underline\">__</em> ");
        test(tests, expects);
    }

    public void testMultiline()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        tests.add("This is not __not\nunderlined__");
        expects.add("This is not __not\nunderlined__");
        test(tests, expects);
    }

    public void testTimeComplexity()
    {
        ArrayList tests = new ArrayList();
        ArrayList expects = new ArrayList();
        // Something like this should be (negatively) matched in linear time, thus it should take no
        // time. If the build takes a lot, then the regular expression is not in linear time, thus
        // wrong.
        String text = "__";
        for (int i = 0; i < 1000; ++i) {
            text += "abc_";
        }
        tests.add(text);
        expects.add(text);
        test(tests, expects);
    }
}
