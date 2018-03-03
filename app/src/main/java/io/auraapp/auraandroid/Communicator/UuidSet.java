package io.auraapp.auraandroid.Communicator;

import android.os.ParcelUuid;

import java.util.UUID;

class UuidSet {
    static final UUID SERVICE = UUID.fromString("47F61476-27FC-450D-BA61-5CA014665C90");
    static final ParcelUuid SERVICE_PARCEL = new ParcelUuid(SERVICE);

    /**
     * Adds a 16 bit UUID from (1) to the BT base UUID as described in (29
     * (1) https://www.bluetooth.com/specifications/assigned-numbers/16-bit-uuids-for-members
     * (2) https://stackoverflow.com/questions/36212020/how-can-i-convert-a-bluetooth-16-bit-service-uuid-into-a-128-bit-uuid
     */
    static final UUID SERVICE_DATA = UUID.fromString("0000FE6E-0000-1000-8000-00805F9B34FB");
//    static final UUID SERVICE_DATA = UUID.fromString("04BF4B10-A922-47C5-84E9-50F2DDD6C9E0"); // random
    static final ParcelUuid SERVICE_DATA_PARCEL = new ParcelUuid(SERVICE_DATA);

    static final UUID USER = UUID.fromString("0A12350F-DEB0-467D-8B6F-1A05FFE9761D");

    static final UUID SLOGAN_1 = UUID.fromString("D58F2517-F61F-4091-A7C7-6842C146A841");
    static final UUID SLOGAN_2 = UUID.fromString("D41B65A3-C25A-4B38-923B-429F508745FE");
    static final UUID SLOGAN_3 = UUID.fromString("3105C799-A563-400B-BC04-A412CE0D98E9");
}
