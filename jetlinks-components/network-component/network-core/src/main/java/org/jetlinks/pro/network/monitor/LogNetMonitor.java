package org.jetlinks.pro.network.monitor;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;

@AllArgsConstructor
public class LogNetMonitor implements NetMonitor {

    private Logger logger;

    @Override
    public void buffered(int size) {
        if(size>0) {
            logger.info("current buffered : {}", size);
        }
    }

    @Override
    public void error(Throwable err) {
        logger.error(err.getMessage(), err);
    }

    @Override
    public void handled() {
    }

    @Override
    public void send() {

    }

    @Override
    public void bytesSent(long bytesLength) {
        logger.info("send number of bytes :{}",bytesLength);
    }

    @Override
    public void bytesRead(long bytesLength) {
        logger.info("receive number of bytes :{}",bytesLength);
    }


    @Override
    public void sendComplete() {

    }


    @Override
    public void sendError(Throwable err) {
        logger.error("send error", err);
    }

    @Override
    public void connected() {
    }

    @Override
    public void disconnected() {
    }
}
