/*
 * Copyright (c) 2021. All rights reserved.
 */

package xzot1k.plugins.hd.api.objects;

import org.bukkit.Location;
import xzot1k.plugins.hd.HyperDrive;

public class SerializableLocation {
    private double x, y, z, yaw, pitch;
    private String worldName;

    public SerializableLocation(org.bukkit.Location location) {
        this.worldName = location.getWorld().getName();
        this.x = location.getX();
        this.y = location.getY();
        this.z = location.getZ();
        this.yaw = location.getYaw();
        this.pitch = location.getPitch();
    }

    public SerializableLocation(String locationString) {
        load(locationString);
    }

    public SerializableLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public SerializableLocation(String worldName, double x, double y, double z) {
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = 0;
        this.pitch = 0;
    }

    public Location asBukkitLocation() {
        return new Location(HyperDrive.getPluginInstance().getServer().getWorld(getWorldName()), getX(), getY(), getZ(), (float) getYaw(), (float) getPitch());
    }

    public double distance(double x, double y, double z) {
        final double differenceInX = (Math.max(x, getX()) - Math.min(x, getX())),
                differenceInY = (Math.max(y, getY()) - Math.min(y, getY())),
                differenceInZ = (Math.max(z, getZ()) - Math.min(z, getZ()));
        return Math.sqrt(Math.pow(differenceInX, 2) + Math.pow(differenceInY, 2) + Math.pow(differenceInZ, 2));
    }

    public double distance(Location location) {
        final double differenceInX = (Math.max(location.getX(), getX()) - Math.min(location.getX(), getX())),
                differenceInY = (Math.max(location.getY(), getY()) - Math.min(location.getY(), getY())),
                differenceInZ = (Math.max(location.getZ(), getZ()) - Math.min(location.getZ(), getZ()));
        return Math.sqrt(Math.pow(differenceInX, 2) + Math.pow(differenceInY, 2) + Math.pow(differenceInZ, 2));
    }

    public double distance(SerializableLocation location) {
        final double differenceInX = (Math.max(location.getX(), getX()) - Math.min(location.getX(), getX())),
                differenceInY = (Math.max(location.getY(), getY()) - Math.min(location.getY(), getY())),
                differenceInZ = (Math.max(location.getZ(), getZ()) - Math.min(location.getZ(), getZ()));
        return Math.sqrt(Math.pow(differenceInX, 2) + Math.pow(differenceInY, 2) + Math.pow(differenceInZ, 2));
    }

    /**
     * Checks if location is equal to another (Exact).
     *
     * @param locationClone The other location.
     * @return Whether the locations are equal.
     */
    public boolean equals(SerializableLocation locationClone) {
        return (locationClone.getWorldName().equalsIgnoreCase(getWorldName()) && getX() == locationClone.getX()
                && getY() == locationClone.getY() && getZ() == locationClone.getZ() && getYaw() == locationClone.getYaw()
                && getPitch() == locationClone.getPitch());
    }

    /**
     * Checks if location is equal to another (Exact).
     *
     * @param location       The other location.
     * @param ignoreRotation Whether to ignore Yaw and Pitch.
     * @return Whether the locations are equal.
     */
    public boolean equals(org.bukkit.Location location, boolean ignoreRotation) {
        return (location.getWorld().getName().equalsIgnoreCase(getWorldName()) && getX() == location.getX()
                && getY() == location.getY() && getZ() == location.getZ()
                && (ignoreRotation || (getYaw() == location.getYaw() && getPitch() == location.getPitch())));
    }

    /**
     * Loads a location from the passed string.
     *
     * @param locationString The string to read.
     */
    public void load(String locationString) {
        if (locationString == null || !locationString.contains(",")) return;
        String[] args = locationString.split(",");
        setWorldName(args[0]);
        setX(Double.parseDouble(args[1]));
        setY(Double.parseDouble(args[2]));
        setZ(Double.parseDouble(args[3]));
        setYaw(Float.parseFloat(args[4]));
        setPitch(Float.parseFloat(args[5]));
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public double getX() {
        return x;
    }

    private void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    private void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    private void setZ(double z) {
        this.z = z;
    }

    public double getYaw() {
        return yaw;
    }

    private void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    private void setPitch(double pitch) {
        this.pitch = pitch;
    }

    @Override
    public String toString() {
        return (getWorldName() + "," + getX() + "," + getY() + "," + getZ() + "," + getYaw() + "," + getPitch());
    }

}
