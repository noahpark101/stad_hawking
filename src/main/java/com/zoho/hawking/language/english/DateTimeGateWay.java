package com.zoho.hawking.language.english;

import com.zoho.hawking.language.AbstractLanguage;
import com.zoho.hawking.language.LanguageFactory;
import com.zoho.hawking.language.english.model.DateGroup;
import com.zoho.hawking.language.english.model.ParserOutput;
import com.zoho.hawking.utils.TimeZoneExtractor;

import com.zoho.hawking.HawkingTimeParser;
import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.datetimeparser.constants.PrepositionConstants;
import com.zoho.hawking.datetimeparser.utils.RelationShipMatching;
import com.zoho.hawking.language.english.model.DateTimeEssentials;
import com.zoho.hawking.language.english.model.DateTimeOffsetReturn;
import com.zoho.hawking.utils.CommonUtils;
import com.zoho.hawking.utils.Constants;
import com.zoho.hawking.utils.DateTimeProperties;
import edu.stanford.nlp.util.Triple;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.Pair;
import org.joda.time.DateTime;

public class DateTimeGateWay {
  private static final Logger LOGGER = Logger.getLogger(HawkingTimeParser.class.getName());

  public static List<DateTimeProperties> getDateAndTime(DateTimeEssentials dateTimeEssentials) throws Exception {
    /*
    Remember that dateTimeEssentials has the essentials (the input string, start, end, tense, etc.)
     */

    List<DateTimeProperties> dateTimePropertiesList = new ArrayList<>();
    AbstractLanguage abstractLanguage = LanguageFactory.getLanguageImpl("eng"); //No I18N
    //gets the dateType (will always be -1 b/c none of them are relation
    int dateType = findDateType(dateTimeEssentials);
    switch (dateType) {
      case 0:
        dateTimePropertiesList = relationshipExtraction(dateTimeEssentials, PrepositionConstants.BETWEEN.getWord(), abstractLanguage);
        break;
      case 1:
        String whatRelation = null;
        for (Triple<String, Integer, Integer> triple : dateTimeEssentials.getTriples()) {
          if (triple.first.equals("R")) {
            whatRelation = dateTimeEssentials.getSentence().substring(triple.second, triple.third);
            break;
          }
        }
        assert whatRelation != null;
        if (whatRelation.equals("or")) {
          dateTimePropertiesList = relationshipExtraction(dateTimeEssentials, PrepositionConstants.OR.getWord(), abstractLanguage);
        } else {
          dateTimePropertiesList = relationshipExtraction(dateTimeEssentials, PrepositionConstants.AND.getWord(), abstractLanguage);
        }
        break;
      case 2:
        Triple<String, Integer, Integer> timeSpan = dateTimeEssentials.getTriples().get(0);
        Triple<String, Integer, Integer> relation = dateTimeEssentials.getTriples().get(1);
        Triple<String, Integer, Integer> exactTimeSpan = dateTimeEssentials.getTriples().get(2);
        DateTimeProperties dateTimePropertiesOne = new DateTimeProperties(dateTimeEssentials, exactTimeSpan);
        dateTimePropertiesOne.setParsedDate();
        Pair<ParserOutput, DateGroup> dateGroupPairOne = HawkingTimeParser.setDateAndTime(dateTimePropertiesOne, abstractLanguage);
        dateTimePropertiesOne.setParserOutput(dateGroupPairOne.getLeft());
        dateTimePropertiesOne.setDateGroup(dateGroupPairOne.getRight());
        String relationWord = dateTimeEssentials.getSentence().substring(relation.second, relation.third);
        switch (relationWord) {
          case "after":
            dateTimeEssentials.setTense("FUTURE"); //No I18N
            break;
          case "before":
            dateTimeEssentials.setTense("PAST"); //No I18N
            break;
          default:
            dateTimeEssentials.setTense("PRESENT"); //No I18N
        }
        DateTimeProperties dateTimePropertiesTwo = new DateTimeProperties(dateTimeEssentials, dateTimePropertiesOne.getParserOutput().getDateRange().getStart(), timeSpan);
        dateTimePropertiesTwo.setParsedDate();

        Pair<ParserOutput, DateGroup> dateGroupPairTwo = HawkingTimeParser.setDateAndTime(dateTimePropertiesTwo, abstractLanguage);
        dateTimePropertiesTwo.setParserOutput(dateGroupPairTwo.getLeft());
        dateTimePropertiesTwo.setDateGroup(dateGroupPairTwo.getRight());

        dateTimePropertiesList.add(dateTimePropertiesTwo);
        break;
      default:
        if (dateTimeEssentials.getTriples().isEmpty()) {
          LOGGER.info("DateTimeGateWay :: NO DATES FOUND");
          break;
        }
        //now, date time properties holds all of dateTimeEssentialls
        DateTimeProperties dateTimeProperties = new DateTimeProperties(dateTimeEssentials, dateTimeEssentials.getTriples().get(0));
        //finally this parses the date string into the actual components of the date/time
        dateTimeProperties.setParsedDate();

        Pair<ParserOutput, DateGroup> dateGroupPair = HawkingTimeParser.setDateAndTime(dateTimeProperties, abstractLanguage);
        dateTimeProperties.setParserOutput(dateGroupPair.getLeft());
        dateTimeProperties.setDateGroup(dateGroupPair.getRight());

        dateTimePropertiesList.add(dateTimeProperties);
    }
    return dateTimePropertiesList;
  }

  private static int findDateType(DateTimeEssentials dateTimeEssentials) {
    int dateType = -1;
    boolean isRelation = dateTimeEssentials.isRelation();
    List<Triple<String, Integer, Integer>> triples = dateTimeEssentials.getTriples();
    if (isRelation) {
      for (Triple<String, Integer, Integer> triple : triples) {
        if (triple.first.equals("R")) {
          String relationWord = dateTimeEssentials.getSentence().substring(triple.second, triple.third).replace("[^a-zA-z]", ""); //No I18N
          if (PrepositionConstants.RELATIONSHIP_RANGE.contains(relationWord.toLowerCase())) {
            dateType = 0;
          } else if (PrepositionConstants.RELATIONSHIP_SET.contains(relationWord.toLowerCase())) {
            dateType = 1;
          } else {
            dateType = 2;
          }
          break;
        }
      }
    }
    return dateType;
  }

  public static org.apache.commons.lang3.tuple.Triple<Boolean, String, String> extractRelation(RelationShipMatching relationShipMatching) {
    String dateComp1 = relationShipMatching.getParsedTextOne();
    String dateComp2 = relationShipMatching.getParsedTextTwo();
    for (Map.Entry<String, String> entry : relationShipMatching.getComponentMapOne().entrySet()) {
      if (entry.getValue() != null) {
        dateComp1 = dateComp1.replace(entry.getValue(), relationShipMatching.getStrTochar().get(entry.getKey()));
      }
    }
    for (Map.Entry<String, String> entry : relationShipMatching.getComponentMapTwo().entrySet()) {
      if (entry.getValue() != null) {
        dateComp2 = dateComp2.replace(entry.getValue(), relationShipMatching.getStrTochar().get(entry.getKey()));
      }
    }

    dateComp1 = dateComp1.replaceAll(relationShipMatching.getRegexRemoveSpecialChar(), "");
    dateComp2 = dateComp2.replaceAll(relationShipMatching.getRegexRemoveSpecialChar(), "");

    String pat1 = dateComp1;
    String pat2 = dateComp2;

    int startIndex;
    String dateCompOne = null;
    String dateCompTwo = null;
    if (pat1.length() == pat2.length()) {
      return org.apache.commons.lang3.tuple.Triple.of(false, null, null);
    } else if (pat1.length() < pat2.length()) {
      startIndex = pat2.indexOf(pat1);
    } else {
      startIndex = pat1.indexOf(pat2);
    }
    String newDateComp;
    boolean check = (startIndex + 1 + pat1.length()) == pat2.length();
    if (startIndex != -1 && check ||
        (startIndex + pat2.length()) == pat1.length()) {
      String rem;
      String cons = "";
      if (check) {
        rem = pat2.substring(startIndex + 1);
        for (char c : rem.toCharArray()) {
          cons = CommonUtils.stringBuild(cons, relationShipMatching.getComponentMapTwo().get(relationShipMatching.getMapInversed().get(c + "")));
        }
        newDateComp = relationShipMatching.getParsedTextOne() + cons;
        newDateComp = newDateComp.replaceAll("> <", "");
        newDateComp = newDateComp.replaceAll("<[^>]*>", " ");
        newDateComp = newDateComp.trim();
        dateCompOne = newDateComp;
      } else {
        rem = pat1.substring(0, startIndex);
        for (char c : rem.toCharArray()) {
          cons = CommonUtils.stringBuild(cons, relationShipMatching.getComponentMapOne().get(relationShipMatching.getMapInversed().get(c + "")));
        }
        newDateComp = cons + relationShipMatching.getParsedTextTwo();
        newDateComp = newDateComp.replaceAll("> <", "");
        newDateComp = newDateComp.replaceAll("<[^>]*>", " ");
        newDateComp = newDateComp.trim();
        dateCompTwo = newDateComp;
      }
    }
    return org.apache.commons.lang3.tuple.Triple.of(true, dateCompOne, dateCompTwo);
  }

  private static List<DateTimeProperties> relationshipExtraction(DateTimeEssentials dateTimeEssentials, String relation, AbstractLanguage abstractLanguage) throws Exception {
    List<DateTimeProperties> dateTimePropertiesList = new ArrayList<>();
    List<Triple<String, Integer, Integer>> triples = dateTimeEssentials.getTriples();
    //for 2 relation words one relation word will be retained in date part to remove that
    String relationWordToRemove = null;
    if (triples.size() > 4) {
      Triple<String, Integer, Integer> relationToRemove = triples.remove(1);
      relationWordToRemove = dateTimeEssentials.getSentence().substring(relationToRemove.second, relationToRemove.third);
      Triple<String, Integer, Integer> tripleDay1 = triples.remove(0);
      Triple<String, Integer, Integer> tripleDay2 = triples.remove(0);
      triples.add(0, Triple.makeTriple("D", tripleDay1.second, tripleDay2.third)); //No I18N
    }
    String relationWord = triples.size() == 3 ? dateTimeEssentials.getSentence().substring(triples.get(1).second, triples.get(1).third) : null;
    Triple<String, Integer, Integer> timeSpanOne;
    Triple<String, Integer, Integer> timeSpanTwo;
    DateTimeProperties relationDateTimePropertiesOne;
    DateTimeProperties relationDateTimePropertiesTwo;
    if (relationWord != null && (relationWord.equals("from") || relationWord.equals("after"))) {
      timeSpanTwo = triples.size() == 3 ? triples.get(0) : triples.get(1);
      timeSpanOne = triples.get(triples.size() - 1);
      relationDateTimePropertiesOne = new DateTimeProperties(dateTimeEssentials, timeSpanOne);
      dateTimeEssentials.addId();
      relationDateTimePropertiesTwo = new DateTimeProperties(getFutureDateEssentials(dateTimeEssentials), new Triple<String, Integer, Integer>(timeSpanTwo.first(), timeSpanTwo.second(), timeSpanTwo.third()));
      if (relationWordToRemove != null) {
        relationDateTimePropertiesOne.cleanParsedText(relationWordToRemove);
      }
      relationDateTimePropertiesOne.setParsedDate();
      relationDateTimePropertiesTwo.setParsedDate();
      RelationShipMatching relationShipMatching = new RelationShipMatching(relationDateTimePropertiesOne.getComponentMap(), relationDateTimePropertiesTwo.getComponentMap(),
          relationDateTimePropertiesOne.getParsedDate().getTaggedWithXML(),
          relationDateTimePropertiesTwo.getParsedDate().getTaggedWithXML());
      if (relationShipMatching.getCanExtract()) {
        org.apache.commons.lang3.tuple.Triple<Boolean, String, String> tripleVal = extractRelation(relationShipMatching);
        if (tripleVal.getLeft()) {
          if (tripleVal.getMiddle() != null) {
            relationDateTimePropertiesOne.setParsedText(tripleVal.getMiddle());
            relationDateTimePropertiesOne.setParsedDate();
          } else if (tripleVal.getRight() != null) {
            relationDateTimePropertiesTwo.setParsedText(tripleVal.getRight());
            relationDateTimePropertiesTwo.setParsedDate();
          }
        }
      }
      Pair<ParserOutput, DateGroup> dateGroupPairOne = HawkingTimeParser.setDateAndTime(relationDateTimePropertiesOne, abstractLanguage);
      relationDateTimePropertiesOne.setParserOutput(dateGroupPairOne.getLeft());
      relationDateTimePropertiesOne.setDateGroup(dateGroupPairOne.getRight());

      relationDateTimePropertiesTwo.setReferenceTime(relationDateTimePropertiesOne.getParserOutput().getDateRange().getStart());
      Pair<ParserOutput, DateGroup> dateGroupPairTwo = HawkingTimeParser.setDateAndTime(relationDateTimePropertiesTwo, abstractLanguage);

      relationDateTimePropertiesTwo.setParserOutput(dateGroupPairTwo.getLeft());
      relationDateTimePropertiesTwo.setDateGroup(dateGroupPairTwo.getRight());
      relationDateTimePropertiesTwo.getDateGroup().setExpression(relationDateTimePropertiesTwo.getDateGroup().getExpression());
      relationDateTimePropertiesTwo.getDateGroup().setSequenceType("SINGLE"); //No I18N
      relationDateTimePropertiesTwo.getParserOutput().setText(relationDateTimePropertiesTwo.getParserOutput().getText()+" "+relationWord+" "+relationDateTimePropertiesOne.getParserOutput().getText()); //No I18N
      relationDateTimePropertiesTwo.getParserOutput().setParserEndIndex(relationDateTimePropertiesOne.getParserOutput().getParserEndIndex());

      dateTimePropertiesList.add(relationDateTimePropertiesTwo);
      return dateTimePropertiesList;
    } else {
      timeSpanOne = triples.size() == 3 ? triples.get(0) : triples.get(1);
      timeSpanTwo = triples.get(triples.size() - 1);
      relationDateTimePropertiesOne = new DateTimeProperties(dateTimeEssentials, timeSpanOne);

      dateTimeEssentials.addId();
      assert relationWord != null;
      if(relationWord.equals("or")||relationWord.equals("-")){
        relationDateTimePropertiesTwo = new DateTimeProperties(getPresentDateEssentials(dateTimeEssentials), new Triple<String, Integer, Integer>(timeSpanTwo.first(), timeSpanOne.third() + 1, timeSpanTwo.third()));
      }
      else {
        relationDateTimePropertiesTwo = new DateTimeProperties(getFutureDateEssentials(dateTimeEssentials), new Triple<String, Integer, Integer>(timeSpanTwo.first(), timeSpanOne.third() + 1, timeSpanTwo.third()));
      }
      relationDateTimePropertiesTwo.cleanParsedText(relationWord);

      relationDateTimePropertiesOne.setParsedDate();
      relationDateTimePropertiesTwo.setParsedDate();

      Pair<ParserOutput, DateGroup> dateGroupPairOne = HawkingTimeParser.setDateAndTime(relationDateTimePropertiesOne, abstractLanguage);
      relationDateTimePropertiesOne.setParserOutput(dateGroupPairOne.getLeft());
      relationDateTimePropertiesOne.setDateGroup(dateGroupPairOne.getRight());

      if (relationDateTimePropertiesTwo.getParsedDate().getTaggedWithXML().contains("current_day")) {
        Pair<ParserOutput, DateGroup> dateGroupPairTwo = HawkingTimeParser.setDateAndTime(relationDateTimePropertiesTwo, abstractLanguage);
        relationDateTimePropertiesTwo.setParserOutput(dateGroupPairTwo.getLeft());
        relationDateTimePropertiesTwo.setDateGroup(dateGroupPairTwo.getRight());
      } else {
        if(relationDateTimePropertiesOne.getParserOutput().getIsTimeZonePresent()){
          HawkingConfiguration configuration= new HawkingConfiguration();
          configuration.setTimeZone("GMT"+relationDateTimePropertiesOne.getParserOutput().getTimezoneOffset()); //No I18N
          DateTimeOffsetReturn dateTimeOffsetReturn = TimeZoneExtractor.referenceDateExtractor(new Date(relationDateTimePropertiesOne.getParserOutput().getDateRange().getStart().getMillis()), configuration, relationDateTimePropertiesTwo.getParserOutput().getText());
          DateTime referenceDate = new DateTime( dateTimeOffsetReturn.getReferenceDate());
          relationDateTimePropertiesTwo.setReferenceTime(referenceDate);
          Pair<ParserOutput, DateGroup> dateGroupPairTwo = HawkingTimeParser.setDateAndTime(relationDateTimePropertiesTwo, abstractLanguage);
          relationDateTimePropertiesTwo.setParserOutput(dateGroupPairTwo.getLeft());
          relationDateTimePropertiesTwo.setDateGroup(dateGroupPairTwo.getRight());
        }
        else {
          relationDateTimePropertiesTwo.setReferenceTime(relationDateTimePropertiesOne.getParserOutput().getDateRange().getStart());
          Pair<ParserOutput, DateGroup> dateGroupPairTwo = HawkingTimeParser.setDateAndTime(relationDateTimePropertiesTwo, abstractLanguage);
          relationDateTimePropertiesTwo.setParserOutput(dateGroupPairTwo.getLeft());
          relationDateTimePropertiesTwo.setDateGroup(dateGroupPairTwo.getRight());
        }
      }

      dateTimePropertiesList.add(relationDateTimePropertiesOne);
      dateTimePropertiesList.add(relationDateTimePropertiesTwo);
      String expr = relationDateTimePropertiesOne.getDateGroup().getExpression() + relation + Constants.OPEN_PARENTHESIS + dateTimeEssentials.getId() + Constants.CLOSE_PARENTHESIS;
      relationDateTimePropertiesOne.getDateGroup().setExpression(expr);
      relationDateTimePropertiesOne.getDateGroup().setSequenceType("MULTIPLE"); //No I18N

      expr = Constants.OPEN_PARENTHESIS + (dateTimeEssentials.getId() - 1) + Constants.CLOSE_PARENTHESIS + relation + relationDateTimePropertiesTwo.getDateGroup().getExpression();
      relationDateTimePropertiesTwo.getDateGroup().setExpression(expr);
      relationDateTimePropertiesTwo.getDateGroup().setSequenceType("MULTIPLE"); //No I18N
      return dateTimePropertiesList;
    }

  }
  private static DateTimeEssentials getFutureDateEssentials(DateTimeEssentials dateTimeEssentials){
    DateTimeEssentials futureDateTimeEssentials =  new DateTimeEssentials(dateTimeEssentials.getParagraph(), dateTimeEssentials.getSentence(), "FUTURE", dateTimeEssentials.getId(),//No I18N
        dateTimeEssentials.getTriples(), dateTimeEssentials.isRelation(), dateTimeEssentials.getReferenceTime(), dateTimeEssentials.getTimeZoneOffSet() );
    return futureDateTimeEssentials;
  }

  private static DateTimeEssentials getPresentDateEssentials(DateTimeEssentials dateTimeEssentials){
    DateTimeEssentials futureDateTimeEssentials =  new DateTimeEssentials(dateTimeEssentials.getParagraph(), dateTimeEssentials.getSentence(), "PRESENT", dateTimeEssentials.getId(),//No I18N
        dateTimeEssentials.getTriples(), dateTimeEssentials.isRelation(), dateTimeEssentials.getReferenceTime(), dateTimeEssentials.getTimeZoneOffSet() );
    return futureDateTimeEssentials;
  }


}

