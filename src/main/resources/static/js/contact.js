console.log("Contact modal script loaded");
const durl="http://localhost:8080";
const viewContactModal = document.getElementById('view_contact_modal');
const options = {
    placement: 'center',
    backdrop: 'dynamic',
    backdropClasses: 'bg-gray-900/50 dark:bg-gray-900/80 fixed inset-0 z-40',
    closable: true,
    onHide: () => {
        console.log('modal is hidden');
    },
    onShow: () => {
        console.log('modal is shown');
    },
    onToggle: () => {
        console.log('modal has been toggled');
    },
};

const instanceOptions = {
    id: 'view_contact_modal',
    override: true
};

const contactModal = new Modal(viewContactModal, options, instanceOptions);

function openContactModal() {
    contactModal.show();
}

function closeContactModal() {
    contactModal.hide();
}

async function loadContactData(id) {
    try {
        console.log("Loading contact:", id);
        
        // Fetch contact data
        const data = await (await fetch(`${durl}/api/contact/${id}`)).json();//.json() converts json to plain javascript objects(like dictionary or hashmap)
        console.log("Contact data:", data);

        // Populate basic info
        document.querySelector("#contact_name").innerHTML = data.name || "Unknown";
        document.querySelector("#contact_email").querySelector("span").innerHTML = data.email || "No email";
        document.querySelector("#contact_phoneNumber").innerHTML = data.phoneNumber || "No phone number";
        document.querySelector("#contact_address").innerHTML = data.address || "No address provided";
        document.querySelector("#contact_description").innerHTML = data.description || "No description available";
        document.querySelector("#contact_id").innerHTML = data.id || "N/A";

        // Set profile image
        const contactImage = document.querySelector("#contact_image");
        if (data.picture && data.picture !== "") {
            contactImage.src = data.picture;
        } else {
            contactImage.src = "/images/user.png";
        }

        // Handle Website Link
        const websiteContainer = document.querySelector("#contact_website_container");
        if (data.websiteLink && data.websiteLink.trim() !== "") {
            websiteContainer.href = data.websiteLink;
            websiteContainer.classList.remove("hidden");
        } else {
            websiteContainer.classList.add("hidden");
        }

        // Handle LinkedIn Link
        const linkedinContainer = document.querySelector("#contact_linkedin_container");
        if (data.linkedInLink && data.linkedInLink.trim() !== "") {
            linkedinContainer.href = data.linkedInLink;
            linkedinContainer.classList.remove("hidden");
        } else {
            linkedinContainer.classList.add("hidden");
        }

        // Show "No links" message if both are empty
        const noLinksMessage = document.querySelector("#contact_no_links");
        const hasWebsite = data.websiteLink && data.websiteLink.trim() !== "";
        const hasLinkedIn = data.linkedInLink && data.linkedInLink.trim() !== "";
        
        if (!hasWebsite && !hasLinkedIn) {
            noLinksMessage.classList.remove("hidden");
        } else {
            noLinksMessage.classList.add("hidden");
        }

        // Handle Favorite badge
        const favoriteBadge = document.querySelector("#contact_favorite");
        if (data.favorite === true) {
            favoriteBadge.classList.remove("hidden");
        } else {
            favoriteBadge.classList.add("hidden");
        }

        // Open the modal
        openContactModal();

    } catch (error) {
        console.error("Error loading contact:", error);
        alert("Failed to load contact details. Please try again.");
    }
}

async function deleteContact(id) {
    Swal.fire({
        title: "Delete Contact?",
        text: "This action cannot be undone!",
        icon: "warning",
        showCancelButton: true,
        confirmButtonColor: "#ef4444", // Red color for delete
        cancelButtonColor: "#6b7280", // Gray color for cancel
        confirmButtonText: '<i class="fa-solid fa-trash mr-2"></i>Delete',
        cancelButtonText: '<i class="fa-solid fa-xmark mr-2"></i>Cancel',
        background: document.documentElement.classList.contains('dark') ? '#1f2937' : '#ffffff',
        color: document.documentElement.classList.contains('dark') ? '#f9fafb' : '#111827',
        customClass: {
    popup: 'rounded-2xl shadow-2xl border border-gray-200 dark:border-gray-700',
    title: 'text-2xl font-bold text-gray-900 dark:text-white',
    htmlContainer: 'text-gray-600 dark:text-gray-400',
    confirmButton: 'px-5 py-2.5 rounded-lg font-semibold hover:bg-red-400 dark:hover:bg-red-600 focus:ring-4 focus:ring-red-300 transition-all duration-200 text-black dark:text-white',
    cancelButton: 'px-5 py-2.5 rounded-lg font-semibold hover:bg-gray-400 dark:hover:bg-gray-600 focus:ring-4 focus:ring-gray-300 transition-all duration-200 text-black dark:text-white'
},
        buttonsStyling: true,
        reverseButtons: true, // Cancel button on left, Delete on right
        focusConfirm: false,
        showClass: {
            popup: 'animate__animated animate__fadeInDown animate__faster'
        },
        hideClass: {
            popup: 'animate__animated animate__fadeOutUp animate__faster'
        }
    }).then((result) => {
        if (result.isConfirmed) {
            // Show loading state
            Swal.fire({
                title: 'Deleting...',
                text: 'Please wait',
                icon: 'info',
                showConfirmButton: false,
                allowOutsideClick: false,
                background: document.documentElement.classList.contains('dark') ? '#1f2937' : '#ffffff',
                color: document.documentElement.classList.contains('dark') ? '#f9fafb' : '#111827',
                didOpen: () => {
                    Swal.showLoading();
                }
            });

            // Construct the URL properly
           const url=`${durl}/user/contact/delete/${id}`;
    window.location.replace(url);
        }
    });
}