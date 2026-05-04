package eu.hxreborn.phdp.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import eu.hxreborn.phdp.R
import eu.hxreborn.phdp.ui.theme.Tokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScaffold(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    summary: String? = null,
    bottomPadding: Dp = 0.dp,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
            snapAnimationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        )

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.padding(start = Tokens.SpacingSm),
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        FilledTonalIconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.padding(start = Tokens.ScreenHorizontalPadding),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    }
                },
                actions = actions,
                expandedHeight = Tokens.LargeAppBarExpandedHeight,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        val mergedPadding =
            PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + bottomPadding,
            )
        if (summary != null) {
            Column {
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier =
                        Modifier.padding(
                            start = Tokens.ScreenHorizontalPadding,
                            end = Tokens.ScreenHorizontalPadding,
                            top = mergedPadding.calculateTopPadding(),
                            bottom = Tokens.SpacingLg,
                        ),
                )
                content(PaddingValues(bottom = mergedPadding.calculateBottomPadding()))
            }
        } else {
            content(mergedPadding)
        }
    }
}
