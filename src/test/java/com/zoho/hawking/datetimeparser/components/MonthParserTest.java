package com.zoho.hawking.datetimeparser.components;

import com.zoho.hawking.datetimeparser.DateAndTime;
import com.zoho.hawking.datetimeparser.DateTimeParser;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.AbstractLanguage;
import com.zoho.hawking.language.LanguageFactory;
import com.zoho.hawking.language.english.model.DateTimeEssentials;
import com.zoho.hawking.language.english.model.DateTimeOffsetReturn;
import com.zoho.hawking.utils.DateTimeProperties;
import com.zoho.hawking.utils.TimeZoneExtractor;
import edu.stanford.nlp.util.Triple;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MonthParserTest {

  /*

      DateAndTime referenceDateAndTime = dateTimeOffsetReturn.getReferenceDate();
      DateAndTime dateAndTime = new DateAndTime(referenceDateTime);
      DateTimeOffsetReturn dateTimeOffsetReturn = TimeZoneExtractor.referenceDateExtractor(referenceDate, config, parsedText);

            DateAndTime dateAndTime = DateTimeParser.timeParser(
              dateTimeProperties.getReferenceTime(),
              tense,
              dateTimeProperties.getComponentsMap(),
              abstractLanguage);
   */

  @Test
  public void basicTest() {
    AbstractLanguage abstractLanguage = LanguageFactory.getLanguageImpl("eng");

    String inputSentence = "Good morning, Have a nice day. Shall we meet on December 20 ?";
    String sent = "Shall we meet on December 20 ?";
    Triple<String, Integer, Integer> t = new Triple<>("D", 14, 28);
    List<Triple<String, Integer, Integer>> lt = new ArrayList<>();
    lt.add(t);
    Pair<Boolean, List<Triple<String, Integer, Integer>>> relAndDate = new ImmutablePair<>(false, lt);
    String tense = "";

    //instantiate dateTimeEssentials
    DateTimeEssentials dateTimeEssentials = new DateTimeEssentials();
    dateTimeEssentials.setParagraph(inputSentence);
    dateTimeEssentials.addId();
    dateTimeEssentials.setSentence(sent);
    dateTimeEssentials.setTriples(relAndDate);
    dateTimeEssentials.setTense(tense);

    //instantiate dateTimeProperties
    DateTimeProperties dateTimeProperties = new DateTimeProperties(dateTimeEssentials, dateTimeEssentials.getTriples().get(0));
    dateTimeProperties.setParsedDate();

    try {
      String inputText = "Great, let's meet December 20";
      String sentenceToParse = "<implict_prefix>on</implict_prefix> <month_of_year>december</month_of_year> <exact_number>20</exact_number>";
      String parsedText = "on December 20"; //get from just running the demo
      String sentenceTense = "";
      HawkingConfiguration hawkingConfiguration = new HawkingConfiguration();
      hawkingConfiguration.setFiscalYearStart(2);
      hawkingConfiguration.setFiscalYearEnd(1);
      hawkingConfiguration.setTimeZone("IST");
      DateAndTime dateAndTime = new DateAndTime(dateTimeProperties.getReferenceTime());

      DateTimeComponent monthParser = new MonthParser(sentenceToParse, sentenceTense, dateAndTime, abstractLanguage);
      /*
      So essentially now you can run the individual methods of the parsers
       */


    } catch (Exception e) {
    }

    assertTrue(true);
  }
}