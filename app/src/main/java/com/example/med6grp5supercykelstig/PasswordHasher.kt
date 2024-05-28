package com.example.med6grp5supercykelstig

import org.mindrot.jbcrypt.BCrypt

class PasswordHasher {
    companion object {
        // Make the function accessible from other scripts
        @JvmStatic
        fun hashPasswordWithSalt(password: String): String {
            val salt = SharedValues.hashingSalt
            return BCrypt.hashpw(password, salt)
        }
    }
}
