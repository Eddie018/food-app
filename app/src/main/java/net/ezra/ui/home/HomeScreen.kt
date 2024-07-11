package net.ezra.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil.compose.rememberImagePainter
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import net.ezra.R
import net.ezra.navigation.*
import net.ezra.ui.products.Product
import net.ezra.ui.products.ProductListItem
import net.ezra.ui.products.fetchProducts

data class Screen(val route: String, val title: String, val icon: Int)

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter", "ResourceAsColor", "UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    var isDrawerOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var locationEnabled by remember { mutableStateOf(true) }
    var productList by remember { mutableStateOf(emptyList<Product>()) }
    var specialOfferList by remember { mutableStateOf(emptyList<Product>()) }
    var isLoading by remember { mutableStateOf(true) }
    var displayedProductCount by remember { mutableStateOf(10) }
    var progress by remember { mutableStateOf(0) }
    var userEmail by remember { mutableStateOf("No Email") }

    val callLauncher: ManagedActivityResultLauncher<Intent, ActivityResult> =
        rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { _ -> }

    LaunchedEffect(Unit) {
        fetchSpecialOffer { specialOffer ->
            specialOfferList = specialOffer
        }
        fetchProducts { fetchedProducts ->
            productList = fetchedProducts
            isLoading = false
        }
    }

    Scaffold(
        topBar = { HomeTopBar(searchQuery, onSearchQueryChange = { searchQuery = it }, locationEnabled, onLocationToggle = { locationEnabled = !locationEnabled }) },
        content = { HomeContent(navController, isDrawerOpen, onDrawerClose = { isDrawerOpen = false }, isLoading, productList, progress) },
        bottomBar = { BottomNavigationBar(navController = navController) },
        floatingActionButtonPosition = FabPosition.Center
    )

    AnimatedDrawer(isOpen = isDrawerOpen, onClose = { isDrawerOpen = false })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(searchQuery: String, onSearchQueryChange: (String) -> Unit, locationEnabled: Boolean, onLocationToggle: () -> Unit) {
    var userName by remember { mutableStateOf("Guest") }
    var profilePictureUrl by remember { mutableStateOf("https://via.placeholder.com/150") } // Default profile picture URL

    // Fetch user data from Firebase Authentication
    LaunchedEffect(Unit) {
        val user = Firebase.auth.currentUser
        if (user != null) {
            userName = user.displayName ?: "No Name"
            profilePictureUrl = user.photoUrl?.toString() ?: profilePictureUrl
        }
    }

    Column(
        modifier = Modifier
            .background(Color(0xFFFF7043))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Good Morning",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Text(
                        text = userName,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = Color.White,
                modifier = Modifier.padding(end = 8.dp)
            )
            BasicTextField(
                value = searchQuery,
                onValueChange = { onSearchQueryChange(it) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .height(50.dp)
                    .padding(horizontal = 16.dp),
            ) { innerTextField ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Looking For Food?",
                            color = Color.Gray,
                            fontSize = 16.sp,
                        )
                    } else {
                        innerTextField()
                    }
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    navController: NavHostController,
    isDrawerOpen: Boolean,
    onDrawerClose: () -> Unit,
    isLoading: Boolean,
    productList: List<Product>,
    progress: Int
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { if (isDrawerOpen) onDrawerClose() }
    ) {
        Spacer(modifier = Modifier.height(80.dp))
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
        ) {
            Spacer(modifier = Modifier.height(160.dp))
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Special Offers", color = Color(0xFFFF7043), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("View All", modifier = Modifier.clickable { navController.navigate(ROUTE_VIEW_SPECIALOFFER) }, color = Color(0xFFFF7043), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            SpecialOffers()

            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Featured Dishes", color = Color(0xFFFF7043), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.weight(1f))
                Text("View All", modifier = Modifier.clickable { navController.navigate(ROUTE_VIEW_PROD) }, color = Color(0xFFFF7043), fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = progress / 100f, color = Color(0xFFFF7043))
                    Text(text = "Loading... $progress%", fontSize = 20.sp, color = Color.Black)
                }
            } else {
                if (productList.isEmpty()) {
                    Text(text = "No products found")
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(productList.take(4)) { product ->
                            ProductListItem(product) {
                                navController.navigate("productDetail/${product.id}")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    // Load More Button
                }
            }
        }
    }
}

@Composable
fun AnimatedDrawer(isOpen: Boolean, onClose: () -> Unit) {
    val drawerWidth = remember { Animatable(if (isOpen) 250f else 0f) }

    LaunchedEffect(isOpen) {
        drawerWidth.animateTo(if (isOpen) 250f else 0f, animationSpec = tween(durationMillis = 300))
    }

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(drawerWidth.value.dp),
        color = Color(0xFFFF7043),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "Dashboard",
                modifier = Modifier
                    .padding(16.dp)
                    .clickable {
                        // Handle click event here
                    },
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Divider(color = Color.Gray)
            Text(
                text = "Settings",
                modifier = Modifier
                    .padding(16.dp)
                    .clickable {
                        // Handle click event here
                    },
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        Screen(route = "home", title = "Home", icon = R.drawable.ic_home),
        Screen(route = "user_dashboard", title = "profile", icon = R.drawable.ic_profile),
        Screen(route = "shopping_cart", title = "Cart", icon = R.drawable.ic_cart),
    )

    BottomNavigation(
        backgroundColor = Color(0xFFFF7043),
        contentColor = Color.White
    ) {
        items.forEach { screen ->
            BottomNavigationItem(
                icon = {
                    Icon(
                        modifier = Modifier
                            .size(30.dp),
                        painter = painterResource(id = screen.icon),
                        contentDescription = screen.title,
                        tint = Color.White
                    )
                },
                label = { Text(text = screen.title, color = Color.White) },
                selected = currentRoute(navController) == screen.route,
                onClick = { navController.navigate(screen.route) },
                alwaysShowLabel = true,
                selectedContentColor = Color.Yellow,
                unselectedContentColor = Color.White
            )
        }
    }
}

private fun currentRoute(navController: NavHostController): String? {
    return navController.currentBackStackEntry?.destination?.route
}

private fun fetchSpecialOffer(callback: (List<Product>) -> Unit) {
    val firestore = Firebase.firestore
    firestore.collection("specialOffers")
        .get()
        .addOnSuccessListener { documents ->
            val specialOffers = documents.map { document ->
                Product(
                    id = document.id,
                    name = document.getString("name") ?: "",
                    description = document.getString("description") ?: "",
                    price = document.getDouble("price") ?: 0.0,
                    imageUrl = document.getString("imageUrl") ?: ""
                )
            }
            callback(specialOffers)
        }
        .addOnFailureListener { exception ->
            exception.printStackTrace()
            callback(emptyList())
        }
}

@Composable
fun SpecialOffers() {
    // Example special offers UI
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(5) { index ->
            Card(
                modifier = Modifier
                    .width(250.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { /* TODO: Handle special offer click */ },
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(colors = listOf(Color(0xFFFF7043), Color.Transparent))),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.sample_food), // Replace with your own image
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "Special Offer $index",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}
