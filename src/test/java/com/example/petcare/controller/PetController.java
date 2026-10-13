package com.example.petcare.controller;

import com.petcare.model.Pet;
import com.petcare.service.PetService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pet CRUD: list (Read), add (Create), edit (Update), delete (Delete). */
@Controller
@RequestMapping("/pets")
public class PetController {

    private final PetService pets;

    public PetController(PetService pets) {
        this.pets = pets;
    }

    /** Options for the species drop-down, available to every view of this controller. */
    @ModelAttribute("speciesOptions")
    public java.util.List<String> speciesOptions() {
        return java.util.List.of("Dog", "Cat", "Bird", "Rabbit", "Hamster", "Other");
    }

    private String uid(HttpSession s) {
        return (String) s.getAttribute("userId");
    }

    @GetMapping
    public String list(HttpSession session, Model model) {
        model.addAttribute("pets", pets.listByOwner(uid(session)));
        return "pets";
    }

    @GetMapping("/new")
    public String newForm(HttpSession session, Model model) {
        model.addAttribute("pet", new Pet(null, uid(session), "", "", "", 0, ""));
        model.addAttribute("editing", false);
        return "pet-form";
    }

    @PostMapping
    public String create(@RequestParam String name, @RequestParam String species,
                         @RequestParam(defaultValue = "") String breed, @RequestParam int age,
                         @RequestParam(defaultValue = "") String medicalHistory,
                         HttpSession session, RedirectAttributes flash) {
        try {
            pets.add(uid(session), name, species, breed, age, medicalHistory);
            flash.addFlashAttribute("success", name + " was added successfully.");
            return "redirect:/pets";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/pets/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable String id, HttpSession session, Model model, RedirectAttributes flash) {
        try {
            model.addAttribute("pet", pets.getOwned(uid(session), id));
            model.addAttribute("editing", true);
            return "pet-form";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/pets";
        }
    }

    @PostMapping("/{id}")
    public String update(@PathVariable String id, @RequestParam String name, @RequestParam String species,
                         @RequestParam(defaultValue = "") String breed, @RequestParam int age,
                         @RequestParam(defaultValue = "") String medicalHistory,
                         HttpSession session, RedirectAttributes flash) {
        try {
            pets.update(uid(session), id, name, species, breed, age, medicalHistory);
            flash.addFlashAttribute("success", "Pet details updated.");
            return "redirect:/pets";
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
            return "redirect:/pets/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable String id, HttpSession session, RedirectAttributes flash) {
        try {
            pets.delete(uid(session), id);
            flash.addFlashAttribute("success", "Pet record deleted.");
        } catch (IllegalArgumentException ex) {
            flash.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/pets";
    }
}
