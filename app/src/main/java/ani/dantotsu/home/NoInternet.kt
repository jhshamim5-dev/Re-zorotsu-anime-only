package ani.dantotsu.home

import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.doOnAttach
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import ani.dantotsu.R
import ani.dantotsu.ZoomOutPageTransformer
import ani.dantotsu.databinding.ActivityNoInternetBinding
import ani.dantotsu.download.anime.OfflineAnimeFragment
// removed: // removed: OfflineAnimeFragment
import ani.dantotsu.initActivity
import ani.dantotsu.navBarHeight
import ani.dantotsu.offline.OfflineFragment
import ani.dantotsu.selectedOption
import ani.dantotsu.settings.saving.PrefManager
import ani.dantotsu.settings.saving.PrefName
import ani.dantotsu.snackString
import ani.dantotsu.themes.ThemeManager
import nl.joery.animatedbottombar.AnimatedBottomBar
import kotlinx.coroutines.launch

class NoInternet : AppCompatActivity() {
    private lateinit var binding: ActivityNoInternetBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ThemeManager(this).applyTheme()

        binding = ActivityNoInternetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // LiquidGlassBottomBar handles its own glass background drawing

        var doubleBackToExitPressedOnce = false
        onBackPressedDispatcher.addCallback(this) {
            if (doubleBackToExitPressedOnce) {
                finishAffinity()
            }
            doubleBackToExitPressedOnce = true
            snackString(this@NoInternet.getString(R.string.back_to_exit))
            Handler(Looper.getMainLooper()).postDelayed(
                { doubleBackToExitPressedOnce = false },
                2000
            )
        }

        binding.root.doOnAttach {
            initActivity(this)
            selectedOption = PrefManager.getVal(PrefName.DefaultStartUpTab)

            binding.includedNavbar.navbarContainer.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = navBarHeight
            }
        }

        // Use AnimatedBottomBar for all themes
        binding.viewpager.visibility = View.VISIBLE
        binding.includedNavbar.root.visibility = View.VISIBLE
        binding.composeMainContent.visibility = View.GONE

        val mainViewPager = binding.viewpager
        val navbar = binding.includedNavbar.navbar as AnimatedBottomBar

        mainViewPager.isUserInputEnabled = false
        mainViewPager.adapter = ViewPagerAdapter(supportFragmentManager, lifecycle)
        mainViewPager.setPageTransformer(ZoomOutPageTransformer())

        navbar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                selectedOption = newIndex
                mainViewPager.setCurrentItem(newIndex, false)
            }
        })

        if (mainViewPager.currentItem != selectedOption) {
            mainViewPager.post {
                mainViewPager.setCurrentItem(selectedOption, false)
                navbar.selectTabAt(selectedOption)
            }
        }
    }

    private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle) {

        override fun getItemCount(): Int = 3

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> OfflineAnimeFragment()
                2 -> OfflineAnimeFragment()
                else -> OfflineFragment()
            }
        }
    }
}

