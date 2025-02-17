use std::fmt::{Display, Formatter};
use std::str::FromStr;

use fondabots_lib::object::Field;
use fondabots_lib::tools::basicize;
use fondabots_lib::ErrType;
use poise::{serenity_prelude as serenity, ChoiceParameter};
use serenity::all::{ButtonStyle, CreateActionRow, CreateButton, Timestamp};
use strum::IntoEnumIterator;
use strum_macros::EnumIter;

use super::Ecrit;

#[derive(EnumIter, Clone, PartialEq, Eq, ChoiceParameter, Debug)]
pub enum Status {
    Ouvert,
    EnAttente,
    Abandonne,
    EnPause,
    SansNouvelles,
    Inconnu,
    Publie,
    Valide,
    Refuse,
    Infraction,
    OuvertPlus
}

impl Display for Status {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", match self {
            Status::Ouvert => "Ouvert",
            Status::EnAttente => "En attente",
            Status::Abandonne => "Abandonné",
            Status::EnPause => "En pause",
            Status::SansNouvelles => "Sans nouvelles",
            Status::Inconnu => "Inconnu",
            Status::Publie => "Publié",
            Status::Valide => "Validé",
            Status::Refuse => "Refusé",
            Status::Infraction => "Infraction",
            Status::OuvertPlus => "Ouvert*"
        })
    }
}

impl FromStr for Status {
    type Err = ErrType;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let basic_s = basicize(s.to_string().as_str());
        Status::iter().find(|v| basicize(v.to_string().as_str()) == basic_s)
            .map_or(Err(ErrType::ObjectNotFound(format!("Statut {s} inexistant."))),
            |v| Ok(v.clone()))
    }
}

impl Field<Ecrit> for Status {
    fn comply_with(obj: &Ecrit, field: &Option<Self>) -> bool {
        if let Some(field) = field {
            return obj.status == *field
        } else {
            true
        }
    }

    fn set_for(obj: &mut Ecrit, field: &Self) {
        obj.status = field.clone();
    }

    fn field_name() -> &'static str {
        "Statut"
    }
}

#[derive(EnumIter, Clone, PartialEq, Eq, ChoiceParameter, Debug)]
pub enum Type {
    Conte,
    #[name = "Idée"]
    Idee,
    Rapport,
    #[name = "Format GdI"]
    FormatGdi,
    Autre
}

impl FromStr for Type {
    type Err = ErrType;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        let basic_s = basicize(s.to_string().as_str());
        Type::iter().find(|v| basicize(v.to_string().as_str()) == basic_s)
            .map_or(Err(ErrType::ObjectNotFound(format!("Type {s} inexistant."))),
                    |v| Ok(v.clone()))
    }
}

impl Display for Type {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        write!(f, "{}", match self {
            Type::Conte => "Conte",
            Type::Idee => "Idée",
            Type::Rapport => "Rapport",
            Type::FormatGdi => "Format GdI",
            Type::Autre => "Autre"
        })
    }
}

impl Field<Ecrit> for Type {
    fn comply_with(obj: &Ecrit, field: &Option<Self>) -> bool {
        if let Some(field) = field {
            return obj.type_ == *field;
        } else {
            true
        }
    }

    fn set_for(obj: &mut Ecrit, field: &Self) {
        obj.type_ = field.clone();
    }

    fn field_name() -> &'static str {
        "Type"
    }
}

impl Type {
    pub fn get_color(&self) -> i32 {
        match self {
            Type::Conte => 0x008000,
            Type::Idee => 0xDF7401,
            Type::Rapport => 0x01A9DB,
            Type::FormatGdi => 0xAE1FF1,
            Type::Autre => 0xFFFFFF,
        }
    }
}


#[derive(Clone, PartialEq, Debug)]
pub struct Interet {
    pub name: String,
    pub date: Timestamp,
    pub type_: String,
    pub member: u64
}

impl Interet {
    pub fn action_row(ecrit_id: u64) -> CreateActionRow {
        CreateActionRow::Buttons(vec![
            CreateButton::new(format!("tm-{ecrit_id}-seul")).label("⊙ Exclusif").style(ButtonStyle::Secondary),
            CreateButton::new(format!("tm-{ecrit_id}-instant")).label("⊟ Immédiat").style(ButtonStyle::Secondary),
            CreateButton::new(format!("tm-{ecrit_id}-ouvert")).label("⋄ Ouvert").style(ButtonStyle::Secondary),
            CreateButton::new(format!("tm-{ecrit_id}-longterme")).label("∙ Intérêt simple").style(ButtonStyle::Secondary),
            CreateButton::new(format!("tm-{ecrit_id}-collab")).label("⋇ Collab recherchée").style(ButtonStyle::Secondary),

        ])
    }

    pub fn get_type(type_str: &str) -> &str {
        match type_str {
            "seul" => "⊙ Exclusif",
            "instant" => "⊟ Immédiat",
            "ouvert" => "⋄ Ouvert",
            "longterme" => "∙ Intérêt simple",
            "collab" => "⋇ Collab recherchée",
            _ => "Inconnu?"
        }
    }
}