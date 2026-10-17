package com.anlian.italase.shared.platform

import androidx.compose.runtime.Composable

/**
 * Returns a callback that closes the app.
 *
 * Android finishes the host Activity (app goes to the launcher, state stays
 * intact so relaunch resumes normally). iOS terminates the process, since
 * iOS has no equivalent of Activity.finish().
 */
@Composable
expect fun rememberAppExit(): () -> Unit