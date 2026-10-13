package com.example.petcare.model;

import com.petcare.util.FileUtil;

/**
 * A registered pet. Extends Animal and adds owner + medical history.
 * Record layout: id|ownerId|name|species|breed|age|medicalHistory
 */
public class Pet extends Animal implements Persistable {

    private final String id;
    private final String ownerId;
    private String medicalHistory;

    public Pet(String id, String ownerId, String name, String species, String breed, int age, String medicalHistory) {
        super(name, species, breed, age);
        this.id = id;
        this.ownerId = ownerId;
        this.medicalHistory = medicalHistory;
    }

    @Override
    public String toRecord() {
        return FileUtil.join(id, ownerId, getName(), getSpecies(), getBreed(), getAge(), medicalHistory);
    }

    @Override public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getMedicalHistory() { return medicalHistory; }
    public void setMedicalHistory(String medicalHistory) { this.medicalHistory = medicalHistory; }
}
