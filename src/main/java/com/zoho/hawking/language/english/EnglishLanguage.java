package com.zoho.hawking.language.english;

import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import com.zoho.hawking.language.AbstractLanguage;
import com.zoho.hawking.language.english.model.DateTimeEssentials;
import com.zoho.hawking.language.english.model.DateTimeOffsetReturn;
import com.zoho.hawking.utils.CoreNlpUtils;
import com.zoho.hawking.utils.DateTimeProperties;
import com.zoho.hawking.utils.TimeZoneExtractor;
import edu.stanford.nlp.util.Triple;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class EnglishLanguage extends AbstractLanguage {
    public EnglishLanguage() {
        super(DateTimeWordProperties.ALL_WORDS);
    }

    private static final Logger LOGGER = Logger.getLogger(EnglishLanguage.class.getName());

    public  List<Pair<Boolean, List<Triple<String, Integer, Integer>>>> getSeparateDates(List<Triple<String, Integer, Integer>> allDates) {
        List<Pair<Boolean, List<Triple<String, Integer, Integer>>>> separateDates = new ArrayList<>();
        int startIndex = 0;
        int endIndex = 0;
        int prevEnd = -1;
        boolean isRelation = false;

        for (Triple<String, Integer, Integer> date : allDates) {
            if (isRelation || date.first().equals("R")) {
                isRelation = true;
            }
            if (prevEnd != -1 && date.second() - prevEnd != 1) {
                List<Triple<String, Integer, Integer>> singleDate = allDates.subList(startIndex, endIndex);
                separateDates.add(Pair.of(isRelation, singleDate));
                startIndex = endIndex;
            }
            prevEnd = date.third();
            endIndex++;
        }
        separateDates.add(Pair.of(isRelation, allDates.subList(startIndex, endIndex)));
        return separateDates;
    }

    @Override
    public List<DateTimeProperties> predict(String inputSentence, Date referenceDate, HawkingConfiguration config) {
        List<DateTimeProperties> dateList = new ArrayList<>();
        /*
            Splits input lang string into sentences
         */
        List<String> inputSentences = CoreNlpUtils.sentenceTokenize(inputSentence);
        int maxParseDates = config.getMaxParseDate(); //max number of dates to parse
        int dateCounter = 0;

        for(String sent: inputSentences){
            /*
                Returns of a list of pairs in this form (relation, object)
                relation is true or false TODO: FLESH THIS OUT
                object is (label, start index, end index)
                the label is what type this text is classified as (in this case label D is date)
                start index is where text starts
                end index is where text ends
                Label D: represents Date
                Label R: represents relation
                Basically, what this does is it looks through the input and finds the separate dates
                In the end you get a list of strings that have a date in them

             */
            List<Pair<Boolean, List<Triple<String, Integer, Integer>>>> singleDatesList = getSeparateDates(Parser.parse(sent));
            //System.out.println(singleDatesList);
            for (Pair<Boolean, List<Triple<String, Integer, Integer>>> relAndDate : singleDatesList) {
                System.out.println("Current sentence: " + sent);
                System.out.println("\t" + relAndDate);
                System.out.println("\tTense: " + getTense(sent));
                //gets the triple
                List<Triple<String, Integer, Integer>> triples = relAndDate.getRight();
                DateTimeEssentials dateTimeEssentials = new DateTimeEssentials();
                //input sentence is the whole input string
                dateTimeEssentials.setParagraph(inputSentence);
                dateTimeEssentials.addId();
                //sent is one sentence from the whole input string we are looking at
                dateTimeEssentials.setSentence(sent);
                dateTimeEssentials.setTriples(relAndDate);
                //tense is just english term for future, past, present, etc. tense
                dateTimeEssentials.setTense(getTense(sent));
                if (!triples.isEmpty()) {
                    Triple<String, Integer, Integer> triple = triples.get(0);

                    int startIndex = triple.second;
                    int endIndex = triple.third;
                    //get the text that holds the date information
                    String parsedText = sent.substring(startIndex, endIndex);
                    //gets the time offset, some dates/times are referenced like 20 seconds ago (this needs a reference time)
                    System.out.println("\tParsed text: " + parsedText);

                    DateTimeOffsetReturn dateTimeOffsetReturn = TimeZoneExtractor.referenceDateExtractor(referenceDate, config, parsedText);
                    if(!TimeZoneExtractor.isTimeZonePresent){
                        dateTimeOffsetReturn = TimeZoneExtractor.referenceDateExtractor(referenceDate, config, sent);
                    }
                    dateTimeEssentials.setReferenceTime(dateTimeOffsetReturn.getReferenceDate());
                    dateTimeEssentials.setTimeZoneOffSet(dateTimeOffsetReturn.getTimeOffset());
                    try {
                        //now actually take the input string that for sure only has 1 date and get info out of it
                        dateList.addAll(DateTimeGateWay.getDateAndTime(dateTimeEssentials));
                        dateCounter += 1;
                        if (maxParseDates != 0 && dateCounter == maxParseDates){
                            return dateList;
                        }
                    } catch (Exception e) {
                        LOGGER.info("HawkingTimeParser :: Exception in Hawking :: Unparsed date component Present");
                    }
                }
            }
        }
        return dateList;

    }

    @Override
    public String getTense(String inputText) {
        return CoreNlpUtils.getParsedDependency(inputText).getTenseClass();
    }
}
