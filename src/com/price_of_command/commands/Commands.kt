package com.price_of_command.commands

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.EveryFrameScriptWithCleanup
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.OfficerDataAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.price_of_command.*
import com.price_of_command.conditions.*
import com.price_of_command.reflection.ReflectionUtils
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommand.CommandResult
import org.lazywizard.console.Console
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.InputImplementation
import java.nio.Buffer
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.IntBuffer
import kotlin.math.roundToInt

class TestCommand : BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        return CommandResult.SUCCESS
    }
}

fun MutableList<OfficerDataAPI>.findByName(name: String): PersonAPI? =
    this.find { officer -> officer.person.nameString.contains(name, true) }?.person

class InjureOfficer : BaseCommand {
    override fun runCommand(officerName: String, ctx: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(officerName)
        if (officer != null) {
            try {
                ConditionManager.addPreconditionOverride(true) { condition ->
                    if (condition.target.nameString == officer.nameString && condition is Injury) {
                        condition.precondition().noop {
                            Outcome.Applied(condition)
                        }
                    } else {
                        null
                    }
                }
                val res = Injury(officer, clock().timestamp, emptyList()).tryInflictAppend()
                Console.showMessage("Injuring officer: $res")
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class GravelyInjureOfficer : BaseCommand {
    override fun runCommand(officer_name: String, p1: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(officer_name)
        if (officer != null) {
            try {
                GraveInjury(officer, ConditionManager.now, emptyList()).tryInflictAppend()
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class HealOfficer : BaseCommand {
    override fun runCommand(allArgs: String, ctx: BaseCommand.CommandContext): CommandResult {
        try {
            val args = allArgs.split("\\s+".toRegex())
            val officerName = args.first()
            val count = args.getOrNull(1)?.toIntOrNull() ?: 1

            val officer = playerOfficers().findByName(officerName) ?: run {
                Console.showMessage("Could not find officer by name")
                return CommandResult.BAD_SYNTAX
            }

            ConditionManager.conditionMap[officer]?.filterIsInstance<Wound>()
                ?.run { subList(0, minOf(this.size, count)) }?.forEach { injury ->
                    injury.expired = true
                }
            return CommandResult.SUCCESS
        } catch (e: Exception) {
            Console.showException("Error: ", e)
        }

        return CommandResult.BAD_SYNTAX
    }
}

class FatigueOfficer : BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(args)

        if (officer != null) {
            try {
                Fatigue(officer, clock().timestamp, emptyList()).tryInflictAppend()
                Console.showMessage("Succeeded fatiguing officer")
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class RestOfficer : BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(args)

        if (officer != null) {
            try {
                val fatigue = ConditionManager.conditionMap[officer]?.filterIsInstance<Fatigue>()?.firstOrNull()
                if (fatigue != null) {
                    fatigue.expired = true
                    Console.showMessage("Succeeded resting officer")
                } else {
                    Console.showMessage("Officer is not fatigued")
                }
            } catch (e: Exception) {
                Console.showException("Error: ", e)
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class KillOfficer : BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        val officer = playerOfficers().findByName(args)

        if (officer != null) {
            val res = Death(officer, clock().timestamp, emptyList()).tryInflictAppend()
            if (res is Outcome.Terminal) {
                Console.showMessage("Succeeded in killing officer")
            } else {
                Console.showMessage("Failed to kill officer")
            }
            return CommandResult.SUCCESS
        }

        Console.showMessage("Could not find officer by name")
        return CommandResult.BAD_SYNTAX
    }
}

class ClickMouse : BaseCommand {
    override fun runCommand(args: String, ctx: BaseCommand.CommandContext): CommandResult {
        val inputImpl = ReflectionUtils.getStatic(Mouse::class.java, "implementation") as InputImplementation

        val inputImplWrapper = InputImplWrapper(inputImpl)
        ReflectionUtils.setStatic(Mouse::class.java, "implementation", inputImplWrapper)

        var (posX, posY) = getMousePosition()

        val steps = 10 / 0.1
        val incr = 200 / steps
        val events = ArrayDeque(
            listOf(
                MouseEvent.leftClickDown(posX, posY), MouseEvent.leftClickUp(posX, posY)
            ) + (0..steps.roundToInt()).map { _ ->
                posY += incr.roundToInt()
                MouseEvent.leftClickDown(posX, posY)
            }.toList() + listOf(MouseEvent.leftClickDown(posX, posY), MouseEvent.leftClickUp(posX, posY, false))
        )

        var wait: IntervalUtil? = IntervalUtil(10f, 10f)
        Global.getSector().addTransientScript(object : EveryFrameScript {
            val timer = IntervalUtil(0.1f, 0.1f)

            override fun isDone() = false

            override fun runWhilePaused() = true

            override fun advance(amount: Float) {
                wait?.also { waiter ->
                    waiter.advance(amount)
                    if (waiter.elapsed in (4.99f..5.01f)) {
                        logger().debug("5s")
                    }
                    if (waiter.intervalElapsed()) {
                        wait = null
                    }
                } ?: run {
                    logger().debug("pumping events")
                    timer.advance(amount)
                    if (timer.intervalElapsed()) {
                        events.removeFirstOrNull()?.let {
                            timer.elapsed = 0f

                            logger().debug(it.toPrettyDebugString())
                            inputImplWrapper.mouseEvent = it
                        }
                    }
                }
            }
        })


        return CommandResult.SUCCESS
    }
}

class RunMouseEvent : BaseCommand {
    companion object {
        var events: List<MouseEvent> = emptyList()
    }

    override fun runCommand(p0: String, p1: BaseCommand.CommandContext): CommandResult {
        Global.getSector().addTransientScript(object : EveryFrameScript {
            val timer = IntervalUtil(2f, 2f)
            val events: MutableList<MouseEvent> = RunMouseEvent.events.toMutableList()
            var start = false

            override fun isDone() = events.isEmpty()

            override fun runWhilePaused() = true

            override fun advance(amount: Float) {
                timer.advance(amount)
                start = start || timer.intervalElapsed()
                if (start) {
                    events.removeFirstOrNull()?.let { event ->
                        (ReflectionUtils.getStatic(
                            Mouse::class.java, "implementation"
                        ) as? InputImplWrapper).also {
                            logger().debug("input wrapper is${it ? "" ? ""} ")
                        }?.let { inputImpl ->
                            inputImpl.mouseEvent = event
                        }
                    }
                }
            }
        })

        return CommandResult.SUCCESS
    }
}

fun addMouseEventLogger() {
    Global.getSector().addTransientScript(object : EveryFrameScript {
        override fun isDone() = false

        override fun runWhilePaused() = true

        override fun advance(amount: Float) {
            val coordBuf = getCoordBuffer()
            val event = MouseEvent(
                RawMouseEvent.fromByteBufferComplete(getReadBuffer()),
                coordBuf[0] with coordBuf[1] with coordBuf[2],
                getMouseBufferCopy()
            )
            if (event.rawMouseEvents.isNotEmpty()) {
                println(event.toPrettyDebugString())
            }
        }
    })
}

fun setMousePosition(x: Int, y: Int) {
    Mouse.setCursorPosition(x, y)
}

fun getMousePosition(): Pair<Int, Int> = Mouse.getX() to Mouse.getY()

fun getMouseBuffer(): ByteBuffer = ReflectionUtils.getStatic(Mouse::class.java, "buttons") as ByteBuffer

fun getCoordBuffer(): IntBuffer = ReflectionUtils.getStatic(Mouse::class.java, "coord_buffer") as IntBuffer

fun getReadBuffer(): ByteBuffer = ReflectionUtils.getStatic(Mouse::class.java, "readBuffer") as ByteBuffer

fun getMouseBufferCopy(): ByteArray {
    val buf = (ReflectionUtils.getStatic(Mouse::class.java, "buttons") as ByteBuffer).duplicate()
    (buf as Buffer).rewind()
    val arr = ByteArray(buf.remaining())
    buf.get(arr)
    return arr
}

private fun setMouseEventButton(idx: Int) {
    ReflectionUtils.setStatic(Mouse::class.java, "eventButton", idx)
}

private fun setMouseEventState(state: Boolean) {
    ReflectionUtils.setStatic(Mouse::class.java, "eventState", state)
}


class InputImplWrapper(
    private val inner: InputImplementation
) : InputImplementation by inner {
    var mouseEvent: MouseEvent? = null

    /**
     * TODO:
     * - Create fake events (have model for events in readBuffer but not coord_buffer or buttons
     * - readBuffer is a queue of events, so prepend our events to the front OR ignore the real impl completely and just write our events normally
     * - need to figure out the coords part of the readBuffer events
     * - probably want to create an abstraction for coord_buffer + does it need to match the coords in readBuffer events?
     * - buttons is simple: it's just the button all the events in readBuffer are for
     * - IMPORTANT: for a click there needs to be a mouse up! aka buttons has a value AND readBuffer has an event with leading 0 byte
     * - nice to have: does writing coordinates directly allow us to avoid Mouse.setCursorPosition or does it just do the same thing?
     *   - tied to implementation somehow? assuming cursor is grabbed aka not borderless
     *   - can we go through the Cursor class instead, and create an entirely new cursor?
     *   - MAYBE unsetting mouse grabbed state aka setGrabbed(false) will fix it?
     */

    override fun readMouse(buf: ByteBuffer) {
        when (val event = mouseEvent?.consume()) {
            null -> inner.readMouse(buf)
            else -> event.readMouse(buf)
        }
    }

    override fun pollMouse(coordBuf: IntBuffer, buttonBuf: ByteBuffer) {
        when (val event = mouseEvent?.consume()) {
            null -> inner.pollMouse(coordBuf, buttonBuf)
            else -> event.pollMouse(coordBuf, buttonBuf)
        }
    }

    // TODO: nice setters for overriding coords, buttons, readBuf. Needs to translate buttons to correct index
}

fun Any?.identityHashCode() = System.identityHashCode(this)

fun ByteBuffer.zero() {
    this.also { (it as Buffer).clear() }.put(ByteArray(this.capacity()) { 0 }).also { (it as Buffer).rewind() }
}

fun ByteBuffer.copyToArray(): ByteArray {
    val dupe = this.duplicate().asReadOnlyBuffer()
    val arr = ByteArray((dupe as Buffer).limit())
    for (idx in arr.indices) {
        arr[idx] = dupe.get(idx)
    }
    return arr
}

data class RawMouseEvent(
    val button: Int,
    val state: Boolean,
    val deltaX: Int,
    val deltaY: Int,
    val deltaScroll: Int,
    val timestampNanos: Long,
) {
    companion object {
        fun fromByteBufferComplete(byteBuffer: ByteBuffer): List<RawMouseEvent> {
            val buf = byteBuffer.duplicate()
            (buf as Buffer).position(0)
            val events = mutableListOf<RawMouseEvent>()
            for (baseIdx in 0 until (buf as Buffer).limit() step Mouse.EVENT_SIZE) {
                events.add(fromByteBuffer(buf))
            }
            return events
        }

        fun fromByteBuffer(buffer: ByteBuffer, duplicate: Boolean = false, position: Int? = null): RawMouseEvent {
            val buf = if (duplicate) {
                buffer.duplicate()
            } else {
                buffer
            }
            position?.also { (buf as Buffer).position(it) }
            return RawMouseEvent(
                buf.get().toInt(), buf.get() > 0, buf.getInt(), buf.getInt(), buf.getInt(), buf.getLong()
            )
        }
    }

    fun writeToByteBuffer(buffer: ByteBuffer, position: Int? = null) {
        position?.let {
            (buffer as Buffer).position(it)
        }
        buffer.put(button.toByte())
        buffer.put(
            if (state) {
                1
            } else {
                0
            }
        )
        buffer.putInt(deltaX)
        buffer.putInt(deltaY)
        buffer.putInt(deltaScroll)
        buffer.putLong(timestampNanos)
    }
}

data class MouseEvent(
    val rawMouseEvents: List<RawMouseEvent>,
    val coords: Triple<Int, Int, Int>,
    val buttons: ByteArray,
    val repeatable: Boolean = true,
) {
    companion object {
        fun leftClickDown(x: Int, y: Int, repeatable: Boolean = false): MouseEvent {
            return MouseEvent(
                listOf(
                    RawMouseEvent(
                        0, true, x, y, 0, System.nanoTime()
                    )
                ), x with y with 0, Buttons(Mouse.getButtonCount(), 1, 0), repeatable
            )
        }

        fun leftClickUp(x: Int, y: Int, repeatable: Boolean = false): MouseEvent {
            return MouseEvent(
                listOf(
                    RawMouseEvent(
                        0, false, x, y, 0, System.nanoTime()
                    )
                ), x with y with 0, Buttons(Mouse.getButtonCount(), 0, 0), repeatable
            )
        }

        fun rightClickDown(x: Int, y: Int, repeatable: Boolean = false): MouseEvent {
            return MouseEvent(
                listOf(
                    RawMouseEvent(
                        1, true, x, y, 0, System.nanoTime()
                    )
                ), x with y with 0, Buttons(Mouse.getButtonCount(), 1, 1), repeatable
            )
        }

        fun rightClickUp(x: Int, y: Int, repeatable: Boolean = false): MouseEvent {
            return MouseEvent(
                listOf(
                    RawMouseEvent(
                        1, false, x, y, 0, System.nanoTime()
                    )
                ), x with y with 0, Buttons(Mouse.getButtonCount(), 0, 1), repeatable
            )
        }
    }

    private var read = false
    private var polled = false
    private val consumed: Boolean
        get() = !read && !polled

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MouseEvent

        if (rawMouseEvents != other.rawMouseEvents) return false
        if (coords != other.coords) return false
        if (!buttons.contentEquals(other.buttons)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = rawMouseEvents.hashCode()
        result = 31 * result + coords.hashCode()
        result = 31 * result + buttons.contentHashCode()
        return result
    }

    fun consume(): WriteableMouseEvent? {
        return if (repeatable || !consumed) {
            object : WriteableMouseEvent {
                override fun readMouse(buf: ByteBuffer) {
                    val buffer = buf as Buffer
                    buffer.clear()
                    for (event in rawMouseEvents) event.writeToByteBuffer(buf)
                    buffer.flip()
                    read = true
                }

                override fun pollMouse(coordBuf: IntBuffer, buttonBuf: ByteBuffer) {
                    coords.toList().forEachIndexed { idx, coord ->
                        coordBuf.put(idx, coord)
                    }
                    buttons.forEachIndexed { index, byte ->
                        buttonBuf.put(index, byte)
                    }
                    polled = true
                }
            }
        } else {
            null
        }
    }

    interface WriteableMouseEvent {
        fun readMouse(buf: ByteBuffer)

        fun pollMouse(coordBuf: IntBuffer, buttonBuf: ByteBuffer)
    }
}

infix fun <A> Pair<A, A>.with(other: A): Triple<A, A, A> {
    return Triple(this.first, this.second, other)
}

infix fun <T> T.with(other: T): Pair<T, T> {
    return this to other
}

class Buttons {
    companion object {
        operator fun invoke(length: Int, vararg bytes: Byte): ByteArray {
            val byteList = bytes.toList()
            if (byteList.size != length) {
                throw Exception("length is $length but given ${byteList.size} bytes")
            }
            return ByteArray(length) { idx ->
                byteList[idx]
            }
        }

        operator fun invoke(length: Int, byte: Byte, index: Int): ByteArray {
            val array = ByteArray(length)
            array[index] = byte
            return array
        }
    }
}
