package io.auraapp.auraandroid.Communicator;

import java.util.UUID;

class UnknownAdvertisementException extends Exception {
    UnknownAdvertisementException(UUID uuid) {
        super("Unknown advertisement with uuid " + uuid.toString());
    }
}
