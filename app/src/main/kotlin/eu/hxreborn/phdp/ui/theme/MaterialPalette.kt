package eu.hxreborn.phdp.ui.theme

// Material Design 2 color palette for user-selectable preferences
object MaterialPalette {
    // Primary colors (500 variants)
    val Red500 = 0xFFF44336.toInt()
    val Pink500 = 0xFFE91E63.toInt()
    val Purple500 = 0xFF9C27B0.toInt()
    val DeepPurple500 = 0xFF673AB7.toInt()
    val Indigo500 = 0xFF3F51B5.toInt()
    val Blue500 = 0xFF2196F3.toInt()
    val LightBlue500 = 0xFF03A9F4.toInt()
    val Cyan500 = 0xFF00BCD4.toInt()
    val Teal500 = 0xFF009688.toInt()
    val Green500 = 0xFF4CAF50.toInt()
    val LightGreen500 = 0xFF8BC34A.toInt()
    val Lime500 = 0xFFCDDC39.toInt()
    val Yellow500 = 0xFFFFEB3B.toInt()
    val Amber500 = 0xFFFFC107.toInt()
    val Orange500 = 0xFFFF9800.toInt()
    val DeepOrange500 = 0xFFFF5722.toInt()
    val Brown500 = 0xFF795548.toInt()

    // Greys
    val Grey = 0xFF808080.toInt()
    val Grey100 = 0xFFF5F5F5.toInt()
    val Grey200 = 0xFFEEEEEE.toInt()
    val Grey300 = 0xFFE0E0E0.toInt()
    val Grey400 = 0xFFBDBDBD.toInt()
    val Grey500 = 0xFF9E9E9E.toInt()
    val Grey600 = 0xFF757575.toInt()
    val Grey700 = 0xFF616161.toInt()
    val Grey800 = 0xFF424242.toInt()
    val Grey900 = 0xFF212121.toInt()

    // Blue Greys
    val BlueGrey100 = 0xFFCFD8DC.toInt()
    val BlueGrey200 = 0xFFB0BEC5.toInt()
    val BlueGrey300 = 0xFF90A4AE.toInt()
    val BlueGrey400 = 0xFF78909C.toInt()
    val BlueGrey500 = 0xFF607D8B.toInt()
    val BlueGrey700 = 0xFF455A64.toInt()
    val BlueGrey800 = 0xFF37474F.toInt()
    val BlueGrey900 = 0xFF263238.toInt()

    // Absolute
    val Black = 0xFF000000.toInt()
    val White = 0xFFFFFFFF.toInt()

    // Preset lists
    val materialColors =
        listOf(
            Red500,
            Pink500,
            Purple500,
            DeepPurple500,
            Indigo500,
            Blue500,
            LightBlue500,
            Cyan500,
            Teal500,
            Green500,
            LightGreen500,
            Lime500,
            Yellow500,
            Amber500,
            Orange500,
            DeepOrange500,
            Brown500,
            Grey500,
            BlueGrey500,
            White,
        )

    val backgroundColors =
        listOf(
            Grey,
            Grey500,
            Grey400,
            Grey300,
            BlueGrey500,
            BlueGrey400,
            BlueGrey300,
            BlueGrey200,
            BlueGrey700,
            BlueGrey800,
            BlueGrey900,
            Grey800,
            Grey700,
            Grey600,
            Grey200,
            Grey100,
            Grey900,
            Black,
            White,
            BlueGrey100,
        )
}
