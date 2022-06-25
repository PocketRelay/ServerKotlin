package com.jacobtread.kme.data

import com.jacobtread.kme.blaze.Packet
import com.jacobtread.kme.blaze.error

/**
 * LoginError Enum for representing the different login error
 * codes as an enum constant.
 *
 * @property value
 * @constructor Create empty LoginError
 */
@Suppress("unused")
enum class LoginError(val value: Int) {
    SERVER_UNAVAILABLE(0x0),
    EMAIL_NOT_FOUND(0xB),
    WRONG_PASSWORD(0xC),
    INVALID_SESSION(0xD),
    EMAIL_ALREADY_IN_USE(0x0F),
    AGE_RESTRICTION(0x10),
    INVALID_ACCOUNT(0x11),
    BANNED_ACCOUNT(0x13),
    INVALID_INFORMATION(0x15),
    INVALID_EMAIL(0x16),
    LEGAL_GUARDIAN_REQUIRED(0x2A),
    CODE_REQUIRED(0x32),
    KEY_CODE_ALREADY_IN_USE(0x33),
    INVALID_CERBERUS_KEY(0x34),
    SERVER_UNAVAILABLE_FINAL(0x4001),
    FAILED_NO_LOGIN_ACTION(0x4004),
    SERVER_UNAVAILABLE_NOTHING(0x4005),
    CONNECTION_LOST(0x4007);

    operator fun invoke(packet: Packet): Packet =
        packet.error(value) {
            text("PNAM", "")
            number("UID", 0)
        }

}


