package com.zoho.hawking.datetimeparser.components;

import com.zoho.hawking.datetimeparser.DateAndTime;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.AbstractLanguage;
import com.zoho.hawking.language.LanguageFactory;
import com.zoho.hawking.language.english.model.DateTimeEssentials;
import com.zoho.hawking.utils.DateTimeProperties;
import edu.stanford.nlp.util.Triple;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthParserTest {

  public AbstractLanguage engLang;
  public HawkingConfiguration hawkConfig;

  @BeforeEach
  public void setup() {
    engLang = LanguageFactory.getLanguageImpl("eng");
    hawkConfig = new HawkingConfiguration();
    hawkConfig.setTimeZone("UTC");
    try {
      hawkConfig.setFiscalYearStart(2);
      hawkConfig.setFiscalYearEnd(1);
    } catch (Exception e) {
      assert(false);
    }
  }

  public MonthParser continueSetup(Triple<String, Integer, Integer> t, String inputSentence, String dateSubstr, String xmlSubstr, String tense) {
    Pair<Boolean, List<Triple<String, Integer, Integer>>> relAndDate = Pair.of(false, List.of(t));

    // Instantiate DateTimeEssentials obj
    DateTimeEssentials dtEssentials = new DateTimeEssentials();
    dtEssentials.setParagraph(inputSentence);
    dtEssentials.addId();
    dtEssentials.setSentence(dateSubstr);
    dtEssentials.setTriples(relAndDate);
    dtEssentials.setTense(tense);

    // Instantiate DateTimeProperties obj
    DateTimeProperties dtProps = new DateTimeProperties(dtEssentials, dtEssentials.getTriples().getFirst());
    dtProps.setParsedDate();

    // Instantiate DateAndTime obj
    DateAndTime dateAndTime = new DateAndTime(dtProps.getReferenceTime());
    return new MonthParser(xmlSubstr, tense, dateAndTime, engLang);
  }

  @Test
  @DisplayName("Basic test")
  public void basicTest() {

    String inputSentence = "Good morning, have a nice day. Shall we meet on December 20 ?";
    String dateSubstr = "Shall we meet on December 20 ?";
    String xmlSubstr = "<implict_prefix>on</implict_prefix> <month_of_year>december</month_of_year> <exact_number>20</exact_number>";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 14, 28);
    String tense = "";

    // Get MonthParser object
    DateTimeComponent monthParser = continueSetup(trip, inputSentence, dateSubstr, xmlSubstr, tense);

    assertTrue(monthParser.isNumberPresent);
    assertTrue(monthParser.isExactTimeSpan);
    assertFalse(monthParser.isSet);
    assertFalse(monthParser.isImmediate);
    assertFalse(monthParser.isOrdinal);
    assertEquals(20, monthParser.number);
    assertEquals(0, monthParser.timeSpanValue);
    assertEquals("december", monthParser.timeSpan);
    assertEquals("", monthParser.sentenceTense);
  }

  @Test
  @DisplayName("Test 2")
  public void test2() {

    String inputSentence = "Good morning, have a nice day. Shall we meet on December 20 ?";
    String dateSubstr = "Shall we meet on December 20 ?";
    String xmlSubstr = "<implict_prefix>on</implict_prefix> <month_of_year>december</month_of_year> <exact_number>20</exact_number>";
    Triple<String, Integer, Integer> trip = new Triple<>("D", 14, 28);
    String tense = "";

    // Get MonthParser object
    DateTimeComponent monthParser = continueSetup(trip, inputSentence, dateSubstr, xmlSubstr, tense);

    monthParser.extractComponentsTags();
  }

}

/* For Matt

      DateAndTime referenceDateAndTime = dateTimeOffsetReturn.getReferenceDate();
      DateAndTime dateAndTime = new DateAndTime(referenceDateTime);
      DateTimeOffsetReturn dateTimeOffsetReturn = TimeZoneExtractor.referenceDateExtractor(referenceDate, config, parsedText);

            DateAndTime dateAndTime = DateTimeParser.timeParser(
              dateTimeProperties.getReferenceTime(),
              tense,
              dateTimeProperties.getComponentsMap(),
              abstractLanguage);
   */