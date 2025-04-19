package com.zoho.hawking;

public class IntegrationTest {
  /*
    High level overview
    HawkingTimeParser.predict()
      1. generates dateTimeEssentials for each date
      2. uses DateTimeGateWay.getDateAndTime() for each dateTimeEssentials generated
      3. DateTimeGateWay uses DateTimeProperties.setParsedDate() to the component map
      4. DateTimeGateWay uses HawkingTimeParser.setDateAndTime() to generate dates
      5. HawkingTimeParser.setDateAndTime() uses the different parser components to do so

      The problem with integration testing is that the entire software is bundled tightly together.

      Maybe we treat it as block box testing ish where I just input strings. But I verify the output by seeing the
      components of the output that correspond to certain parts of the code?
        1. like date range to verify date range is working well
        2. maybe the text to see that NLP is properly segmenting it?
        Need more ideas.

   */
}
