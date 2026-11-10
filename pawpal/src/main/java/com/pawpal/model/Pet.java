package com.pawpal.model;

/**
 * Generic pet (species OTHER). {@link Dog}, {@link Cat}, {@link Bird} override the species-specific behaviour
 * (POLYMORPHISM); fields stay private (ENCAPSULATION).
 */
public class Pet extends BaseEntity {
    private long ownerId;
    private String name = "";
    private String breed = "";
    private int ageYears;
    private Gender gender = Gender.UNKNOWN;
    private String notes = "";
    private String photoFile = "";

    public Species getSpecies() { return Species.OTHER; }
    /** Short care tip shown on the pet detail page. */
    public String getCareTip() { return "Schedule a general check-up at least once a year."; }
    public int getTypicalLifespanYears() { return 10; }
    public boolean isSenior() { return ageYears >= getTypicalLifespanYears() * 0.7; }

    public long getOwnerId() { return ownerId; }
    public void setOwnerId(long ownerId) { this.ownerId = ownerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public int getAgeYears() { return ageYears; }
    public void setAgeYears(int ageYears) { this.ageYears = ageYears; }
    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }
    public boolean hasPhoto() { return photoFile != null && !photoFile.isBlank(); }
    public String getPhotoFile() { return photoFile; }
    public void setPhotoFile(String photoFile) { this.photoFile = photoFile == null ? "" : photoFile; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
