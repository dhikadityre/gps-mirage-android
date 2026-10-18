package com.dhikadityre.gpsmirage.feature.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhikadityre.dev.tools.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.more_about_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HERO CARD: APPLICATION & VERSION HIGHLIGHT
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_gps_mirage_logo),
                        contentDescription = null,
                        modifier = Modifier.size(76.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = uiState.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = stringResource(R.string.about_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(18.dp))

                    // HERO PROMINENT VERSION CARD
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.about_version, uiState.versionName),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.about_build_target, uiState.versionCode, uiState.targetSdk),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.about_min_sdk, uiState.minSdk), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text(stringResource(R.string.about_tag_offline), fontSize = 11.sp) }
                        )
                    }
                }
            }

            // SECTION 1: TUJUAN APLIKASI
            AboutInfoCard(
                icon = Icons.Default.TrackChanges,
                title = stringResource(R.string.about_app_purpose_title),
                accentColor = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = stringResource(R.string.about_app_purpose_content),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.about_features_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BulletPoint(stringResource(R.string.about_feature_1))
                    BulletPoint(stringResource(R.string.about_feature_2))
                    BulletPoint(stringResource(R.string.about_feature_3))
                    BulletPoint(stringResource(R.string.about_feature_4))
                }
            }

            // SECTION 2: DEVELOPER DI BALIKNYA
            AboutInfoCard(
                icon = Icons.Default.Code,
                title = stringResource(R.string.about_developer_title),
                accentColor = MaterialTheme.colorScheme.secondary
            ) {
                Text(
                    text = uiState.developerName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.about_developer_role),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.about_developer_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // SECTION 3: KEAMANAN & PRIVASI DATA
            AboutInfoCard(
                icon = Icons.Default.Security,
                title = stringResource(R.string.about_security_title),
                accentColor = MaterialTheme.colorScheme.tertiary
            ) {
                Text(
                    text = stringResource(R.string.about_security_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // FOOTER
            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun AboutInfoCard(
    icon: ImageVector,
    title: String,
    accentColor: androidx.compose.ui.graphics.Color,
    content: @Composable ColumnScope.() -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "• ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
