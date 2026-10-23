package com.anlian.italase.shared.data.local

import cocoapods.SQLCipher.sqlite3_key
import cocoapods.SQLCipher.thisSymbolDefinitelyDoesNotExist

internal fun interopProbe(): Int = sqlite3_key(null, null, 0) + thisSymbolDefinitelyDoesNotExist()
