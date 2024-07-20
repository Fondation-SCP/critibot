use std::fmt::{Display, Formatter};

use poise::{ChoiceParameter, serenity_prelude as serenity};
use serenity::all::{ButtonStyle, CreateActionRow, CreateButton, Timestamp};
use strum::IntoEnumIterator;
use strum_macros::{EnumIter};
use fondabots_lib::tools::basicize;

#[derive(EnumIter, Clone, PartialEq, ChoiceParameter, Debug)]
pub enum Status {
    Ouvert,
    #[name = "En Attente"]
    EnAttente,
    Abandonne,
    #[name = "En pause"]
    EnPause,
    #[name = "Sans nouvelles"]
    SansNouvelles,
    Inconnu,
    #[name = "Publié"]
    Publie,
    #[name = "Validé"]
    Valide,
    #[name = "Refusé"]
    Refuse,
    Infraction,
    #[name = "Ouvert*"]
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

impl From<&str> for Status {
    fn from(value: &str) -> Self {
        for ref v in Status::iter() {
            let vstr: String = v.to_string();
            if basicize(vstr.as_str()) == basicize(value.to_string().as_str()) {
                return v.clone()
            }
        }
        eprintln!("Avertissement : conversion invalide de &str ({value}) vers Status. Défaut à Inconnu.");
        Status::Inconnu
    }
}
/*
#[async_trait::async_trait]
impl SlashArgument for Status {
    async fn extract(_ctx: &SerenityContext, _interaction: &CommandInteraction, value: &ResolvedValue<'_>) -> Result<Self, SlashArgError> {
        if let ResolvedValue::String(s) = value {
            Ok(Self::from(*s))
        } else {
            Err(SlashArgError::new_command_structure_mismatch("Pas une chaîne de caractères."))
        }
    }

    fn create(builder: CreateCommandOption) -> CreateCommandOption {
        builder.kind(CommandOptionType::String)
    }

    fn choices() -> Vec<CommandParameterChoice> {
        let mut ret = Vec::new();
        for item in Self::iter() {
            ret.push(CommandParameterChoice {
                name: item.to_string(),
                localizations: hashmap! {
                    "fr".to_string() => item.to_string()
                },
                __non_exhaustive: (),
            });
        }
        ret
    }
}

 */

#[derive(EnumIter, Clone, PartialEq, ChoiceParameter, Debug)]
pub enum Type {
    Conte,
    #[name = "Idée"]
    Idee,
    Rapport,
    #[name = "Format GdI"]
    FormatGdi,
    Autre
}
/*
#[async_trait::async_trait]
impl SlashArgument for Type {
    async fn extract(_ctx: &SerenityContext, _interaction: &CommandInteraction, value: &ResolvedValue<'_>) -> Result<Self, SlashArgError> {
        if let ResolvedValue::String(s) = value {
            Ok(Self::from(*s))
        } else {
            Err(SlashArgError::new_command_structure_mismatch("Pas une chaîne de caractères."))
        }
    }

    fn create(builder: CreateCommandOption) -> CreateCommandOption {
        builder.kind(CommandOptionType::String)
    }

    fn choices() -> Vec<CommandParameterChoice> {
        let mut ret = Vec::new();
        for item in Self::iter() {
            ret.push(item.to_string());
        }
        ret
    }
}

 */

impl From<&str> for Type {
    fn from(value: &str) -> Self {
        for ref v in Type::iter() {
            let vstr: String = v.to_string();
            if basicize(vstr.as_str()) == basicize(value.to_string().as_str()) {
                return v.clone()
            }
        }
        eprintln!("Avertissement : conversion invalide de &str ({value}) vers Type. Défaut à Autre.");
        Type::Autre
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