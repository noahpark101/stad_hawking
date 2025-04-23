//$Id$
package com.zoho.hawking;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.english.model.DatesFound;


class HawkingDemo {

  private static final Logger LOGGER = LoggerFactory
      .getLogger(HawkingDemo.class);

  public static void main(String[] args) throws Exception {
    /*
      HawkingTimeParser.parse ->
      EnglishLanguage.predict ->
      DateTimeGateway.getDateAndTime ->
      HawkingTimePraser.setDateAndTime ->
      DateTimeParser.timeParser ->
      calls all the individual components

      Every component requires this:
      String sentenceToParse, String sentenceTense, DateAndTime dateAndTime, AbstractLanguage abstractLanguage)

      How to get these:
      sentenceToParse -> sentence to parse (it is in the tag <> form) ex
      <implict_prefix>on</implict_prefix> <month_of_year>december</month_of_year> <exact_number>20</exact_number>
      to get this, just run a sentence through to get the sentenceToParse
      DateAndTime (just put in new Date(null) i think)
      sentenceTense (look at CoreMlpUtils.java getTense for these just put in a random one)
      AbstractLanguage abstractLanguage = LanguageFactory.getLanguageImpl(lang); lang is "eng"
     */
    HawkingTimeParser parser = new HawkingTimeParser();
    String inputText = "Oh I see. It is in a few days.";
    HawkingConfiguration hawkingConfiguration = new HawkingConfiguration();
    hawkingConfiguration.setTimeZone("EDT");
    Date referenceDate = new Date(1745164800000L); // 4/20/2025
    DatesFound datesFound = null;
    try {
      datesFound = parser.parse(inputText,
              referenceDate, hawkingConfiguration, "eng");
    } catch (Exception e) {
      e.printStackTrace();
    }
    assert datesFound != null;
    LOGGER.info("DATES FOUND ::  " + datesFound.toString());
  }

}
