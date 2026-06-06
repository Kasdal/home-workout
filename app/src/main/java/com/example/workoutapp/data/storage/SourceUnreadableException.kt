package com.example.workoutapp.data.storage

import java.io.IOException

class SourceUnreadableException(
    message: String = "Photo source could not be opened."
) : IOException(message)
