from django.urls import path

from core import views

urlpatterns = [
    path("", views.transaction_list, name="list"),
    path("add/", views.transaction_create, name="add"),
    path("<int:pk>/", views.transaction_detail, name="detail"),
    path("<int:pk>/edit/", views.transaction_edit, name="edit"),
]


