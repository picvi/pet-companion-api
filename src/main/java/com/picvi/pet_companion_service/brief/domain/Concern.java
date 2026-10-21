package com.picvi.pet_companion_service.brief.domain;

import com.picvi.pet_companion_service.petlog.api.PetLogApi.LogSnapshot;
import com.picvi.pet_companion_service.petlog.api.LogPredicates;

import java.util.Arrays;
import java.util.List;

public enum Concern {
    DIARRHOEA {
        @Override
        public boolean matches(LogSnapshot log) {
            return LogPredicates.looseStool(log);
        }
    },
    VOMITING {
        @Override
        public boolean matches(LogSnapshot log) {
            return LogPredicates.vomiting(log);
        }
    },
    LOW_APPETITE {
        @Override
        public boolean matches(LogSnapshot log) {
            return LogPredicates.leftFood(log);
        }
    },
    LOW_ENERGY {
        @Override
        public boolean matches(LogSnapshot log) {
            return LogPredicates.lowEnergy(log);
        }
    },
    OTHER {
        @Override
        public boolean matches(LogSnapshot log) {
            return false;
        }
    };

    public abstract boolean matches(LogSnapshot log);

    /** Concerns that can come from logs; OTHER is the owner's free text only. */
    public static List<Concern> fromLogs() {
        return Arrays.stream(values()).filter(concern -> concern != OTHER).toList();
    }
}
