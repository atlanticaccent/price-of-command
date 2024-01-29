package com.price_of_command.conditions.scars.personality_change

import com.fs.starfarer.api.impl.campaign.ids.Personalities

abstract class SpecStub<T : SpecStub<T>>(marker: Class<T>, val id: String) : Level1<T>(marker)

class Timid : SpecStub<Timid>(Timid::class.java, Personalities.TIMID)

class Cautious : SpecStub<Cautious>(Cautious::class.java, Personalities.CAUTIOUS)

class Steady : SpecStub<Steady>(Steady::class.java, Personalities.STEADY)

class Aggressive : SpecStub<Aggressive>(Aggressive::class.java, Personalities.AGGRESSIVE)

class Reckless : SpecStub<Reckless>(Reckless::class.java, Personalities.RECKLESS)
