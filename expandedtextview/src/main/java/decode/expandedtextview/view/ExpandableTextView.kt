package decode.expandedtextview.view

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import decode.expandedtextview.R

private const val READ_MORE_TEXT = "more"
private const val READ_LESS_TEXT = "less"
private const val SEPARATOR = "..."
private const val NUMBER_OF_LINES_BEFORE_EXPANDING = 3
private const val ANIMATION_DURATION = 500L

typealias MeasuredHeight = Pair<Int, Int>

val MeasuredHeight.startHeight
    get() = first

val MeasuredHeight.endHeight
    get() = second

@SuppressWarnings("TooManyFunctions")
class ExpandableTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr), View.OnClickListener {

    companion object {
        const val ANIMATION_PROPERTY_MAX_HEIGHT = "maxHeight"
        const val MAX_VALUE_ALPHA = 255
        const val MIN_VALUE_ALPHA = 0
        const val ANIMATION_PROPERTY_ALPHA = "alpha"
    }

    private var _readMoreText: CharSequence = READ_MORE_TEXT
    private var _readLessText: CharSequence = READ_LESS_TEXT
    private var _noLinesBeforeExpanding: Int = NUMBER_OF_LINES_BEFORE_EXPANDING
    private var _noLinesShowing: Int = Int.MAX_VALUE
    private var _isExpanded: Boolean = false
    private var _highlightColor: Int = Color.BLACK

    private var foregroundColor: Int = 0
    private var initialText: String = ""

    private val visibleText: String by lazy {
        var end = 0
        for (i in 0 until _noLinesShowing) {
            if (layout.getLineEnd(i) == 0) break
            else end = layout.getLineEnd(i)
        }
        text.substring(0, end - _readMoreText.length)
    }

    init {
        context.withStyledAttributes(attrs, R.styleable.ExpandableTextView) {
            getString(R.styleable.ExpandableTextView_readLabel)?.let { _readMoreText = it }
            getInt(
                R.styleable.ExpandableTextView_numberOfLinesBeforeTruncate,
                NUMBER_OF_LINES_BEFORE_EXPANDING
            ).let { _noLinesBeforeExpanding = it }
            getInt(
                R.styleable.ExpandableTextView_numberOfLinesShowing,
                Int.MAX_VALUE
            ).let { _noLinesShowing = it }
            getBoolean(R.styleable.ExpandableTextView_isExpanded, false).let { _isExpanded = it }
            getColor(
                R.styleable.ExpandableTextView_highlightedColor,
                Color.BLUE
            ).let { _highlightColor = it }
        }
        setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        if (!visibleText.isAllTextVisible()) {
            toggleExpandText()
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        setForeground()
        if (initialText.isBlank()) {
            initialText = text.toString()
            if (lineCount > _noLinesBeforeExpanding) ellipsize()
        }
    }

    private fun ellipsize() {
        text = if (_isExpanded || visibleText.isAllTextVisible()) {
            SpannableStringBuilder(initialText)
                .append(SEPARATOR)
                .append(_readLessText.toString().span())
        } else {
            val truncatedText = visibleText.substring(0, visibleText.length - readMoreLength())
            SpannableStringBuilder(truncatedText)
                .append(SEPARATOR)
                .append(_readMoreText.toString().span())
        }
    }

    private fun toggleExpandText() {
        _isExpanded = !_isExpanded

        setAnimation(getHeightMeasures()).apply {
            duration = ANIMATION_DURATION
            start()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(p0: Animator?) {
                    if (!_isExpanded) ellipsize()
                }

                override fun onAnimationStart(p0: Animator?) = Unit
                override fun onAnimationCancel(p0: Animator?) = Unit
                override fun onAnimationRepeat(p0: Animator?) = Unit
            })
        }

        ellipsize()
    }

    private fun setForeground() {
        if (checkForAndroidVersion()) {
            foreground = GradientDrawable(
                GradientDrawable.Orientation.BOTTOM_TOP,
                intArrayOf(foregroundColor, Color.TRANSPARENT)
            )
            foreground.alpha =
                if (_isExpanded) MIN_VALUE_ALPHA
                else MAX_VALUE_ALPHA
        }
    }

    private fun setAnimation(measuredHeight: MeasuredHeight): AnimatorSet {
        return AnimatorSet().apply {
            if (checkForAndroidVersion()) {
                playTogether(
                    ObjectAnimator.ofInt(
                        this,
                        ANIMATION_PROPERTY_MAX_HEIGHT,
                        measuredHeight.startHeight,
                        measuredHeight.endHeight
                    ),
                    ObjectAnimator.ofInt(
                        this@ExpandableTextView.foreground,
                        ANIMATION_PROPERTY_ALPHA,
                        foreground.alpha,
                        MAX_VALUE_ALPHA - foreground.alpha
                    )
                )
            }
        }
    }

    // region Helpers
    private fun checkForAndroidVersion(version: Int = Build.VERSION_CODES.M): Boolean =
        Build.VERSION.SDK_INT >= version

    private fun readMoreLength() = _readMoreText.toString().length + SEPARATOR.length

    private fun getHeightMeasures(): MeasuredHeight {
        val startHeight = measuredHeight
        measure(
            MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )
        return startHeight to measuredHeight
    }

    private fun String.isAllTextVisible() = this == text

    private fun String.span(): SpannableString =
        SpannableString(this).apply {
            setSpan(
                ForegroundColorSpan(_highlightColor),
                0,
                this.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

    // endregion
}
