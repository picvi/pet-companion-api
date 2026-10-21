package com.picvi.pet_companion_service.petlog.domain;

import java.util.Set;

public record LogSpec(boolean detailRequired, Set<LogField> required, Set<LogField> allowed) {
}
