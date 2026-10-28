package com.picvi.pet_companion_service.nutrition.digest;

import com.picvi.pet_companion_service.nutrition.domain.IngredientCode;
import com.picvi.pet_companion_service.nutrition.domain.ProteinStatus;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Groups the owner-added products of a day by normalised name and writes the email. */
public class FoodDigestFormatter {

    public List<DigestEntry> entries(List<DigestInput> inputs) {
        Map<String, List<DigestInput>> groups = new LinkedHashMap<>();
        inputs.forEach(input -> groups.computeIfAbsent(input.normalisedName(), key -> new ArrayList<>()).add(input));
        List<DigestEntry> entries = new ArrayList<>();
        for (List<DigestInput> group : groups.values()) {
            DigestInput first = group.get(0);
            Set<UUID> owners = group.stream().map(DigestInput::ownerId).collect(Collectors.toCollection(LinkedHashSet::new));
            List<IngredientCode> proteins = group.stream()
                    .flatMap(input -> input.mainProteins().stream())
                    .distinct()
                    .toList();
            boolean notSure = group.stream().anyMatch(input -> input.proteinStatus() == ProteinStatus.UNKNOWN);
            entries.add(new DigestEntry(typedName(first), first.form(), first.complete(), proteins, notSure, owners.size(),
                    group.stream().map(DigestInput::productId).toList()));
        }
        return entries;
    }

    public String subject(List<DigestEntry> entries) {
        return "curl up: " + entries.size() + (entries.size() == 1 ? " new owner-added food" : " new owner-added foods");
    }

    public String body(List<DigestEntry> entries) {
        StringBuilder text = new StringBuilder("New foods added by owners since the last digest:\n\n");
        for (DigestEntry entry : entries) {
            text.append("x").append(entry.owners()).append("  ").append(entry.name()).append('\n');
            text.append("    form: ").append(entry.form()).append(", complete: ").append(entry.complete()).append('\n');
            text.append("    proteins: ").append(this.proteins(entry)).append('\n');
            text.append("    product ids: ").append(entry.productIds().stream().map(UUID::toString).collect(Collectors.joining(", "))).append("\n\n");
        }
        text.append("Review them with GET /admin/food-products/pending, then POST /admin/food-products/{id}/approve or /reject.\n");
        return text.toString();
    }

    private String proteins(DigestEntry entry) {
        String named = entry.proteins().stream().map(Enum::name).collect(Collectors.joining(", "));
        if (entry.proteinNotSure()) {
            return named.isEmpty() ? "not sure" : named + " (some owners are not sure)";
        }
        return named;
    }

    private static String typedName(DigestInput input) {
        return java.util.stream.Stream.of(input.brand(), input.productName(), input.flavour())
                .filter(part -> part != null && !part.isBlank())
                .collect(Collectors.joining(" "));
    }
}
