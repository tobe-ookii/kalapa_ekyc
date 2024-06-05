package vn.kalapa.ekyc.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import java.util.concurrent.TimeUnit


open class BaseFragment: Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // we don't need to change transition animations when method calls again!

        // we don't need to change transition animations when method calls again!
        if (savedInstanceState != null) return

        // here we pause enter transition animation to load view completely

        // here we pause enter transition animation to load view completely
        postponeEnterTransition()

        // we set the background color of root view to white
        // because navigation animations run on a transparent background by default

        // we set the background color of root view to white
        // because navigation animations run on a transparent background by default
//        view.setBackgroundColor(Color.WHITE)

        // here we start transition using a handler
        // to make sure transition animation won't be lagged

        // here we start transition using a handler
        // to make sure transition animation won't be lagged
        view.post { postponeEnterTransition(0, TimeUnit.MILLISECONDS) }
    }
}