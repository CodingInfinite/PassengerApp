package com.spartons.passengerapp.interfaces

import com.spartons.passengerapp.model.Driver


interface FirebaseDriverListener {

    fun onDriverAdded(driver: Driver)

    fun onDriverRemoved(driver: Driver)

    fun onDriverUpdated(driver: Driver)
}