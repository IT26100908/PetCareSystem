package com.example.petcare.model;

/** INHERITANCE: generic animal data shared by every kind of pet. */
public abstract class Animal {

    private String name;
    private String species;
    private String breed;
    private int age;

    protected Animal(String name, String species, String breed, int age) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    /** Lower-case key used by the UI for colours: dog, cat, bird, rabbit, hamster or other. */
    public String getSpeciesKey() {
        String s = species == null ? "" : species.toLowerCase();
        if (s.contains("dog")) return "dog";
        if (s.contains("cat")) return "cat";
        if (s.contains("bird") || s.contains("parrot")) return "bird";
        if (s.contains("rabbit")) return "rabbit";
        if (s.contains("hamster")) return "hamster";
        return "other";
    }

    /** Emoji avatar for the species (unicode escapes keep the source file encoding-safe). */
    public String getEmoji() {
        switch (getSpeciesKey()) {
            case "dog":     return "\uD83D\uDC36";
            case "cat":     return "\uD83D\uDC31";
            case "bird":    return "\uD83D\uDC26";
            case "rabbit":  return "\uD83D\uDC30";
            case "hamster": return "\uD83D\uDC39";
            default:        return "\uD83D\uDC3E";
        }
    }

    /** Senior animals (8+ years) need extra care and cost more at the vet. */
    public boolean isSenior() { return age >= 8; }
}
