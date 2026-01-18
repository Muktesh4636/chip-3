package com.transactionhub.ui.more

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.transactionhub.R

class AppearanceFragment : Fragment() {
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_appearance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("AppearancePrefs", android.content.Context.MODE_PRIVATE)

        view.findViewById<TextView>(R.id.appearanceTitle).text = "Appearance & Settings"

        setupAppearanceSettings(view)
    }

    private fun setupAppearanceSettings(view: View) {
        val radioGroupTheme = view.findViewById<RadioGroup>(R.id.radioGroupTheme)
        val radioLight = view.findViewById<RadioButton>(R.id.radioLight)
        val radioDark = view.findViewById<RadioButton>(R.id.radioDark)
        val radioSystem = view.findViewById<RadioButton>(R.id.radioSystem)

        // Load saved theme preference
        val savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        when (savedTheme) {
            AppCompatDelegate.MODE_NIGHT_NO -> radioLight.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES -> radioDark.isChecked = true
            else -> radioSystem.isChecked = true
        }

        radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.radioLight -> AppCompatDelegate.MODE_NIGHT_NO
                R.id.radioDark -> AppCompatDelegate.MODE_NIGHT_YES
                else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }

            // Save preference
            prefs.edit().putInt("theme_mode", mode).apply()

            // Apply theme
            AppCompatDelegate.setDefaultNightMode(mode)

            Toast.makeText(context, "Theme applied! Restart app for full effect.", Toast.LENGTH_SHORT).show()
        }

        // Language settings
        val spinnerLanguage = view.findViewById<Spinner>(R.id.spinnerLanguage)
        val languages = arrayOf("English", "Hindi", "Spanish", "French", "German")
        val adapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, languages)
        spinnerLanguage.adapter = adapter

        // Load saved language
        val savedLangIndex = prefs.getInt("language_index", 0)
        spinnerLanguage.setSelection(savedLangIndex)

        spinnerLanguage.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("language_index", position).apply()
                if (position > 0) {
                    Toast.makeText(context, "Language change requires app restart", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Font size settings
        val spinnerFontSize = view.findViewById<Spinner>(R.id.spinnerFontSize)
        val fontSizes = arrayOf("Small", "Medium", "Large", "Extra Large")
        val fontAdapter = ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_dropdown_item, fontSizes)
        spinnerFontSize.adapter = fontAdapter

        val savedFontSize = prefs.getInt("font_size_index", 1) // Default to Medium
        spinnerFontSize.setSelection(savedFontSize)

        spinnerFontSize.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putInt("font_size_index", position).apply()
                Toast.makeText(context, "Font size will be applied on next app start", Toast.LENGTH_SHORT).show()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Other settings
        val switchAnimations = view.findViewById<Switch>(R.id.switchAnimations)
        val switchAutoBackup = view.findViewById<Switch>(R.id.switchAutoBackup)

        switchAnimations.isChecked = prefs.getBoolean("animations_enabled", true)
        switchAutoBackup.isChecked = prefs.getBoolean("auto_backup_enabled", false)

        switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("animations_enabled", isChecked).apply()
            Toast.makeText(context, "Animations: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }

        switchAutoBackup.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_backup_enabled", isChecked).apply()
            Toast.makeText(context, "Auto backup: ${if (isChecked) "Enabled" else "Disabled"}", Toast.LENGTH_SHORT).show()
        }
    }
}