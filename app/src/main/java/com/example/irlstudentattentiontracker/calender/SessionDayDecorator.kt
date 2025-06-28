import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.style.ForegroundColorSpan
import androidx.core.content.ContextCompat
import com.example.irlstudentattentiontracker.R
import com.prolificinteractive.materialcalendarview.DayViewDecorator
import com.prolificinteractive.materialcalendarview.DayViewFacade
import com.prolificinteractive.materialcalendarview.CalendarDay
import java.util.HashSet

class SessionDayDecorator(
    private val sessionDates: HashSet<CalendarDay>,
    private val context: Context // Pass context from your activity/fragment
) : DayViewDecorator {

    override fun shouldDecorate(day: CalendarDay): Boolean {
        return sessionDates.contains(day)
    }

    override fun decorate(view: DayViewFacade) {
        val color = ContextCompat.getColor(context, R.color.toolbarBg)
        view.setBackgroundDrawable(ColorDrawable(color))
        view.addSpan(ForegroundColorSpan(Color.WHITE))
    }
}
