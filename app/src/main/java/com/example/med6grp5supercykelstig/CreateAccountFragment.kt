package com.example.med6grp5supercykelstig

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

@SuppressLint("StaticFieldLeak")
private lateinit var createUsernameInput: EditText
@SuppressLint("StaticFieldLeak")
private lateinit var createEmailInput: EditText
@SuppressLint("StaticFieldLeak")
private lateinit var createPasswordInput: EditText

val dbHost = SharedValues.dbHost
val dbPort = SharedValues.dbPort
val dbName = SharedValues.dbName
val dbUser = SharedValues.dbUser
val dbPassword = SharedValues.dbPassword



/**
 * A simple [Fragment] subclass.
 * Use the [CreateAccountFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class CreateAccountFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_create_account, container, false)

        // Find the register button in the layout
        val registerButton = view.findViewById<Button>(R.id.registerAccountBtn)

        // Set OnClickListener for the button
        registerButton.setOnClickListener {
            // Call the method to handle button click
            onRegisterAccClick()
        }

        createUsernameInput = view.findViewById(R.id.createUsernameET)
        createEmailInput = view.findViewById(R.id.createEmailET)
        createPasswordInput = view.findViewById(R.id.createPasswordET)

        return view
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment CreateAccountFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            CreateAccountFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun onRegisterAccClick() {
        // Get the text from the EditText components
        val username = createUsernameInput.text.toString()
        val email = createEmailInput.text.toString()
        val password = createPasswordInput.text.toString()
        val hashedPassword = PasswordHasher.hashPasswordWithSalt(password)
        var ctx = requireContext()

        if (username == "" || password == ""){
            // Terminate the onRegisterAccClick() method if username or password are empty
            showToast(ctx, "Failed to register account.\nPlease make sure to fill in username and password.")
            return
        }
        else{
        // Construct the query string for insertion
        val query = "INSERT INTO users (username, email, password_hash) VALUES ('$username', '$email', '$hashedPassword')"
        // Execute the query using DBQueryFacilitator AsyncTask
        val dbquery = DBQueryFacilitator({ success, resultValue ->
            if (success) {
                // Registration successful, display toast message
                showToast(ctx, "Account registration successful")
            } else {
                // Registration failed, display an error message
                showToast(ctx, "Account registration failed.")
            }
        }, true) // Pass true for executeUpdate (INSERT, UPDATE, DELETE), false for executeQuery (SELECT)

        dbquery.execute(
            "-host",
            dbHost,
            "-port",
            dbPort,
            "-database",
            dbName,
            "-username",
            dbUser,
            "-password",
            dbPassword,
            "-query",
            query
        )


        if (activity is UserLoginActivity) {
            (activity as UserLoginActivity).onFragmentDismissed()
        }
        // Get the fragment manager
        val fragmentManager = parentFragmentManager

        // Remove or replace the fragment
        fragmentManager.beginTransaction().remove(this).commit()
    }}

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

}