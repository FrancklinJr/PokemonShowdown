from django.urls import path
from batalha import views

urlpatterns = [
    path('', views.index, name='index'),
]
