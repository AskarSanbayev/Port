package com.askar.port.service;

import com.askar.port.entity.Delivery;
import com.askar.port.entity.Port;
import com.askar.port.entity.Ship;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public class DeliveryController {

    private static final Logger LOGGER = LogManager.getLogger(DeliveryController.class);
    private Port port;
    private Condition isFull;

    public DeliveryController(Port port){
        this.port = port;
        isFull = port.getLock().newCondition();
    }

    public void deliverToPort(Port port, Ship ship) {
        port.getLock().lock();
        try {
            LOGGER.info(ship.getModel() + " delivering containers to port");
            while (ship.getContainerCount() != 0) {
                port.loadContainers(ship.getContainerCount());
                ship.deliverContainers(ship.getContainerCount());
                while (port.getContainerCount() >= Port.CAPACITY / 2 + 500)
                    try {
                        LOGGER.info("Port is full,waiting for a container service " + ship.getModel());
                        isFull.await();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info("Finished delivering to port " + ship.getModel());
        } finally {
            port.getLock().unlock();
        }
    }


    public void loadFromPort(Port port, int containerAmount) {
        port.getLock().lock();
        try {
            LOGGER.info("Getting containers from port");
            while (port.getContainerCount() >= Delivery.MIN_CONTAINER_AMOUNT) {
                port.unloadContainers(containerAmount);
            }
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            LOGGER.info("Port is free,continue delivering");
            isFull.signal();
        } finally {
            port.getLock().unlock();
        }
    }
}
