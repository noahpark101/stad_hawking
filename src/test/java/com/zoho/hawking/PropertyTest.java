package com.zoho.hawking;

import com.zoho.hawking.datetimeparser.configuration.HawkingConfiguration;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class PropertyTest {
  private HawkingConfiguration hawkingConfiguration;
  private HawkingTimeParser parser;
  private Date referenceDate;
  private String defaultStartDate;

  /**
   * Gets the date given a Start or End line
   * @param str the string in the following format: "Start/End : {Date}:{Time}"
   * @return the Date
   */
  private String getDate(String str) {
    List<String> ls = Arrays.asList(str.split("T"));
    return ls.get(0);
  }

  @BeforeEach
  void setUp() {
    hawkingConfiguration = new HawkingConfiguration();
    parser = new HawkingTimeParser();
    referenceDate = new Date(120, Calendar.DECEMBER, 1);
    defaultStartDate = "2020-12-01";
  }
}
