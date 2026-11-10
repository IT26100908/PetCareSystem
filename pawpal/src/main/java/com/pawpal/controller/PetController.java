package com.pawpal.controller;

import com.pawpal.exception.BusinessRuleException;
import com.pawpal.exception.ValidationException;
import com.pawpal.form.PetForm;
import com.pawpal.model.Gender;
import com.pawpal.model.Pet;
import com.pawpal.model.Species;
import com.pawpal.service.*;
import com.pawpal.storage.ImagePhotoStorage;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.util.concurrent.TimeUnit;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Module 02: /pets */
@Controller
@RequestMapping("/pets")
public class PetController {
    private final PetService petService;
    private final UserService userService;
    private final AppointmentService appointmentService;
    private final MedicalRecordService medicalService;
    private final LookupService lookup;
    private final PetHistoryService historyService;

    public PetController(PetService petService, UserService userService, AppointmentService appointmentService,
                         MedicalRecordService medicalService, LookupService lookup, PetHistoryService historyService) {
        this.petService = petService;
        this.userService = userService;
        this.appointmentService = appointmentService;
        this.medicalService = medicalService;
        this.lookup = lookup;
        this.historyService = historyService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) Species species,
                       @RequestParam(required = false) String sort, Model model) {
        model.addAttribute("pets", petService.search(q, species, sort));
        model.addAttribute("sort", sort == null ? "name" : sort);
        model.addAttribute("exportQuery", com.pawpal.util.QueryString.of("q", q, "species", species, "sort", sort));
        model.addAttribute("q", q);
        model.addAttribute("species", species);
        model.addAttribute("speciesList", Species.values());
        model.addAttribute("ownerNames", lookup.userNames());
        return "pets/list";
    }

    @GetMapping("/new")
    public String newPet(Model model) {
        model.addAttribute("form", new PetForm());
        return form(model, null);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") PetForm form, BindingResult br, Model model, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                Pet p = petService.create(form);
                ra.addFlashAttribute("success", p.getName() + " was added.");
                return "redirect:/pets/" + p.getId();
            } catch (ValidationException e) { FormErrors.add(br, e); }
        }
        return form(model, null);
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        Pet pet = petService.getAccessible(id);
        model.addAttribute("pet", pet);
        model.addAttribute("ownerName", lookup.userNames().getOrDefault(pet.getOwnerId(), "—"));
        model.addAttribute("appointments", appointmentService.findByPet(id));
        model.addAttribute("records", medicalService.findRecordsByPet(id));
        model.addAttribute("vaccinations", medicalService.findVaccinationsByPet(id));
        model.addAttribute("vetNames", lookup.vetNames());
        model.addAttribute("serviceNames", lookup.serviceNames());
        return "pets/detail";
    }

    /** Streams the pet's photo to its owner or an admin (403 / 404 otherwise). */
    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable long id) {
        ImagePhotoStorage.StoredPhoto p = petService.loadPhoto(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.contentType()))
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(p.bytes());
    }

    /** One timeline of appointments, medical records and vaccinations for this pet. */
    @GetMapping("/{id}/history")
    public String history(@PathVariable long id, @RequestParam(required = false) PetHistoryService.Kind kind, Model model) {
        model.addAttribute("pet", petService.getAccessible(id));
        model.addAttribute("entries", historyService.history(id, kind));
        model.addAttribute("kind", kind);
        model.addAttribute("kinds", PetHistoryService.Kind.values());
        return "pets/history";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable long id, Model model) {
        Pet p = petService.getAccessible(id);
        PetForm f = new PetForm();
        f.setName(p.getName());
        f.setSpecies(p.getSpecies());
        f.setBreed(p.getBreed());
        f.setAgeYears(p.getAgeYears());
        f.setGender(p.getGender());
        f.setNotes(p.getNotes());
        f.setOwnerId(p.getOwnerId());
        model.addAttribute("form", f);
        return form(model, id);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable long id, @Valid @ModelAttribute("form") PetForm form, BindingResult br,
                         Model model, RedirectAttributes ra) {
        if (!br.hasErrors()) {
            try {
                petService.update(id, form);
                ra.addFlashAttribute("success", "Pet updated.");
                return "redirect:/pets/" + id;
            } catch (ValidationException e) { FormErrors.add(br, e); }
        } else petService.getAccessible(id);   // keep 403/404 behaviour even when the form is invalid
        return form(model, id);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable long id, RedirectAttributes ra) {
        try {
            petService.delete(id);
            ra.addFlashAttribute("success", "Pet deleted.");
            return "redirect:/pets";
        } catch (BusinessRuleException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/pets/" + id;
        }
    }

    private String form(Model model, Long id) {
        model.addAttribute("editId", id);
        model.addAttribute("pet", id == null ? null : petService.getAccessible(id));     // current photo preview
        model.addAttribute("maxPhotoMb", ImagePhotoStorage.MAX_BYTES / (1024 * 1024));
        model.addAttribute("speciesList", Species.values());
        model.addAttribute("genders", Gender.values());
        if (userService.currentUser().isAdmin()) model.addAttribute("owners", userService.findAll());
        return "pets/form";
    }
}
