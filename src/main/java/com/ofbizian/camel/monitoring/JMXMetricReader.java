package com.ofbizian.camel.monitoring;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.StandardUnit;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * User: bilgin.ibryam
 */
public class JMXMetricReader implements Processor {
    private static final Logger LOGGER = LoggerFactory.getLogger(JMXMetricReader.class);
    private MBeanServer mBeanServer;

    @Override
    public void process(Exchange exchange) throws Exception {
        if (mBeanServer == null) {
            mBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
        }
        Long exchangesFailed = (Long) getAttributeFromJMX("routes", "processingRoute", "ExchangesFailed", "monitorContext");
        Long exchangesCompleted = (Long) getAttributeFromJMX("routes", "processingRoute", "ExchangesCompleted", "monitorContext");
        Long maxProcessingTime = (Long) getAttributeFromJMX("routes", "processingRoute", "MaxProcessingTime", "monitorContext");
        Long meanProcessingTime = (Long) getAttributeFromJMX("routes", "processingRoute", "MeanProcessingTime", "monitorContext");

        List<MetricDatum> metrics = new ArrayList<MetricDatum>();
        metrics.add(new MetricDatum()
                .withMetricName("ExchangesFailed")
                .withValue(exchangesFailed.doubleValue())
                .withUnit(StandardUnit.Count));

        metrics.add(new MetricDatum()
                .withMetricName("ExchangesCompleted")
                .withValue(exchangesCompleted.doubleValue())
                .withUnit(StandardUnit.Count));

        metrics.add(new MetricDatum()
                .withMetricName("MaxProcessingTime")
                .withValue(maxProcessingTime.doubleValue())
                .withUnit(StandardUnit.Milliseconds));

        metrics.add(new MetricDatum()
                .withMetricName("MeanProcessingTime")
                .withValue(meanProcessingTime.doubleValue())
                .withUnit(StandardUnit.Milliseconds));

        exchange.getIn().setBody(metrics);
    }

    private Object getAttributeFromJMX(String type, String beanName, String attributeName, String camelContext) {
        try {
            ObjectName objectName = createObjectName(type, beanName, camelContext);
            return mBeanServer.getAttribute(objectName, attributeName);
        } catch (Exception e) {
            LOGGER.debug("Attribute not found", e);
            return null;
        }
    }

    private ObjectName createObjectName(final String type, final String beanName, final String camelContext) throws MalformedObjectNameException {
        String objectNameString = "org.apache.camel:context=" + getHostName() + "/" + camelContext + ",type=" + type + ",name=\"" + beanName + "\"";
        return new ObjectName(objectNameString);
    }

    private String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException uhe) {
            LOGGER.error("Error getting host name", uhe);
            String host = uhe.getMessage();
            if (host != null) {
                int colon = host.indexOf(':');
                if (colon > 0) {
                    String hostName = host.substring(0, colon);
                    LOGGER.debug("Found host name {}", hostName, uhe);
                    return hostName;
                }
            }
            return "localhost";
        }
    }
}
