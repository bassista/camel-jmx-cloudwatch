package com.ofbizian.camel.monitoring;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import java.util.Random;

/**
 * User: ibryab01
 */
public class TweetAnalyzer implements Processor {
    Random generator = new Random();

    @Override
    public void process(Exchange exchange) throws Exception {

         //analyze hard
        Thread.sleep(generator.nextInt(500));
    }
}
