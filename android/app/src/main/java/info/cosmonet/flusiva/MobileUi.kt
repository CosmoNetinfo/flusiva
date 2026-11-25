package info.cosmonet.flusiva

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.Button
import android.widget.ImageButton

object MobileUi {
    val blue = Color.rgb(23, 98, 244)
    val ink = Color.rgb(11, 37, 75)
    val muted = Color.rgb(91, 111, 141)
    val pale = Color.rgb(246, 249, 255)
    fun dp(context: Context, value: Int) = (value * context.resources.displayMetrics.density).toInt()
    fun touch(context: Context, primary: Boolean = false, radius: Int = 14): RippleDrawable {
        val states = arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_selected), intArrayOf())
        val colors = intArrayOf(if (primary) 0xffdfe7f4.toInt() else Color.TRANSPARENT, 0xffe5efff.toInt(), if (primary) blue else Color.TRANSPARENT)
        val shape = GradientDrawable().apply { setColor(ColorStateList(states, colors)); cornerRadius = dp(context, radius).toFloat() }
        val mask = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = dp(context, radius).toFloat() }
        return RippleDrawable(ColorStateList.valueOf(if (primary) 0x44ffffff else 0x221762f4), shape, mask)
    }
    fun button(context: Context, label: String, primary: Boolean = false, action: () -> Unit): Button = Button(context).apply {
        text = label; isAllCaps = false; textSize = 14f; typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        minHeight = 0; minWidth = 0; minimumHeight = dp(context, 48); minimumWidth = dp(context, 48)
        setPadding(dp(context, 12), dp(context, 4), dp(context, 12), dp(context, 4)); backgroundTintList = null
        background = touch(context, primary); elevation = 0f; stateListAnimator = null
        setTextColor(ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()), intArrayOf(muted, if (primary) Color.WHITE else blue)))
        setOnClickListener { action() }
    }
    fun icon(context: Context, name: String, label: String, size: Int = 48, primary: Boolean = false, action: () -> Unit): ImageButton = ImageButton(context).apply {
        contentDescription = label; setImageDrawable(MusicIcon(name, if (primary) Color.WHITE else ink)); setPadding(dp(context, if (size > 60) 20 else 12), dp(context, if (size > 60) 20 else 12), dp(context, if (size > 60) 20 else 12), dp(context, if (size > 60) 20 else 12))
        backgroundTintList = null; background = touch(context, primary, size / 2); elevation = 0f; stateListAnimator = null
        imageTintList = ColorStateList(arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf(android.R.attr.state_selected), intArrayOf()), intArrayOf(0xffa5b3c8.toInt(), blue, if (primary) Color.WHITE else ink))
        minimumHeight = dp(context, size); minimumWidth = dp(context, size); setOnClickListener { action() }
    }
    fun text(view: android.widget.TextView, value: CharSequence) { if (view.text.toString() != value.toString()) view.text = value }
}
