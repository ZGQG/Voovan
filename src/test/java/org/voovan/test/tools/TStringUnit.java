package org.voovan.test.tools;

import junit.framework.TestCase;
import org.voovan.tools.TObject;
import org.voovan.tools.TString;

import java.util.Map;

public class TStringUnit extends TestCase {

	String simpleStr ="^ helyho is my name ^";
	public TStringUnit(String name) {
		super(name);
	}

	public void testRemovePrefix() {
		String resultStr = TString.removePrefix(simpleStr);
		assertEquals(resultStr, " helyho is my name ^");
	}

	public void testRemoveSuffix() {
		String resultStr = TString.removeSuffix(simpleStr);
		assertEquals(resultStr, "^ helyho is my name ");
	}

	public void testLeftPad() {
		String resultStr = TString.leftPad(simpleStr, 25, '-');
		assertEquals(resultStr, "----^ helyho is my name ^");
	}

	public void testRightPad() {
		String resultStr = TString.rightPad(simpleStr, 25, '-');
		assertEquals(resultStr, "^ helyho is my name ^----");
	}

	public void testIsNumber() {
		boolean test = TString.isNumber("10", 10);
		assertTrue(test);
		test = TString.isNumber("1A", 10);
		assertTrue(!test);
		test = TString.isNumber("1A", 16);
		assertTrue(test);
	}

	public void testIsInteger() {
		assertTrue(TString.isInteger("1"));
		assertTrue(!TString.isInteger("1.0"));
	}

	public void testIsFloat() {
		assertTrue(TString.isFloat("1.0"));
		assertTrue(!TString.isFloat("1"));
	}

	public void testSearchByRegex() {
		assertTrue(TString.regexMatch(simpleStr, "helyho")==1);
	}

	public void testIsNullOrEmpty() {
		assertTrue(TString.isNullOrEmpty(""));
		assertTrue(TString.isNullOrEmpty(null));
		assertTrue(!TString.isNullOrEmpty("str"));
	}

	@SuppressWarnings("unchecked")
	public void testTokenReplaceStringMapOfStringString() {
		String simpleTokenStr ="^ {{helyho}} {{is}} my name ^";
		Map<String, String> tokens = TObject.newMap("helyho","HELY HO","is","IS'NT");
		String replacedStr = TString.tokenReplace(simpleTokenStr, tokens);
		assertEquals(replacedStr,"^ HELY HO IS'NT my name ^");
	}

	public void testTokenReplaceStringStringString() {
		String simpleTokenStr ="^ {{helyho}} is my name ^";	
		String replacedStr = TString.tokenReplace(simpleTokenStr, "helyho","HELY HO");
		assertEquals(replacedStr,"^ HELY HO is my name ^");
	}

	public void testFormat() {
		String formatStr = "aaaa{}bbbb{}cccc{}";
		String formatedStr = TString.format(formatStr, "1","2","x");
		assertEquals(formatedStr,"aaaa1bbbb2ccccx");
	}

	public void testReplaceFirst() {
		String formatStr = "aaaa{}bbbb{}cccc{}";
		String formatedStr = TString.replaceFirst(formatStr,"{}", "1");
		assertEquals(formatedStr,"aaaa1bbbb{}cccc{}");
	}

	public void testReplaceLast() {
		String formatStr = "aaaa{}bbbb{}cccc{}";
		String formatedStr = TString.replaceLast(formatStr,"{}", "1");
		assertEquals(formatedStr,"aaaa{}bbbb{}cccc1");
	}
}
